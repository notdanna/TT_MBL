"""
evaluar_test.py

Evalúa imágenes de prueba comparando las predicciones de:
  1. El modelo Keras (.keras)
  2. El modelo cuantizado TFLite (.tflite)

Genera una tabla comparativa en consola y exporta un archivo CSV con los resultados.

Uso típico:
    python src/inferencia/evaluar_test.py
    python src/inferencia/evaluar_test.py --test_dir data/test_samples --salida reports/resultados_test.csv
"""

import os
os.environ["TF_USE_LEGACY_KERAS"] = "1"
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import argparse
from pathlib import Path
import numpy as np
import pandas as pd
from PIL import Image
from tqdm import tqdm
import tensorflow as tf
import tf_keras as legacy_keras

# Directorio raíz del proyecto (ModelTT)
PROJECT_ROOT = Path(__file__).resolve().parents[2]

# Configuración GPU
gpus = tf.config.list_physical_devices("GPU")
if gpus:
    try:
        for _gpu in gpus:
            tf.config.experimental.set_memory_growth(_gpu, True)
    except RuntimeError:
        pass


def preprocesar_imagen(ruta_imagen: Path, img_size: int = 300) -> np.ndarray:
    """Carga y redimensiona una imagen para inferencia."""
    img_pil = Image.open(ruta_imagen).convert("RGB")
    img_array_bruto = np.array(img_pil)

    img_tensor = tf.convert_to_tensor(img_array_bruto, dtype=tf.float32)
    img_tensor = tf.image.resize(img_tensor, [img_size, img_size])
    img_final = tf.expand_dims(img_tensor, axis=0)

    return img_final.numpy()


def parsear_argumentos() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--test_dir",
        type=Path,
        default=PROJECT_ROOT / "data" / "test_samples",
        help="Carpeta que contiene imágenes para prueba/inferencia.",
    )
    parser.add_argument(
        "--keras_model",
        type=Path,
        default=PROJECT_ROOT / "models" / "mejor_modelo_fase2.keras",
        help="Ruta al modelo Keras guardado (.keras).",
    )
    parser.add_argument(
        "--tflite_model",
        type=Path,
        default=PROJECT_ROOT / "models" / "bark_classifier.tflite",
        help="Ruta al modelo cuantizado TFLite (.tflite).",
    )
    parser.add_argument(
        "--salida",
        type=Path,
        default=PROJECT_ROOT / "reports" / "resultados_test.csv",
        help="Ruta donde se guardará el CSV con los resultados de inferencia.",
    )
    parser.add_argument(
        "--img_size",
        type=int,
        default=300,
        help="Tamaño de entrada para las imágenes (por defecto 300).",
    )
    parser.add_argument(
        "--umbral",
        type=float,
        default=0.5,
        help="Umbral de decisión para clasificar como Infestado (>= umbral).",
    )
    return parser.parse_args()


def resolver_ruta(ruta: Path) -> Path:
    """Resuelve la ruta respecto a CWD o respecto a PROJECT_ROOT si no existe."""
    if ruta.exists():
        return ruta
    alternativa = PROJECT_ROOT / ruta
    if alternativa.exists():
        return alternativa
    return ruta


def main():
    args = parsear_argumentos()

    test_dir = resolver_ruta(args.test_dir)
    keras_model_path = resolver_ruta(args.keras_model)
    tflite_model_path = resolver_ruta(args.tflite_model)
    salida_csv = args.salida if args.salida.is_absolute() else (PROJECT_ROOT / args.salida)

    if not test_dir.exists():
        print(f"Error: No existe la carpeta de pruebas '{test_dir}'.")
        return
    if not keras_model_path.exists():
        print(f"Error: No se encontró el modelo Keras en '{keras_model_path}'.")
        return
    if not tflite_model_path.exists():
        print(f"Error: No se encontró el modelo TFLite en '{tflite_model_path}'.")
        return

    print("=== CONFIGURACIÓN DE INFERENCIA ===")
    print(f"Carpeta de imágenes : {test_dir}")
    print(f"Modelo Keras        : {keras_model_path}")
    print(f"Modelo TFLite       : {tflite_model_path}")
    print(f"Umbral de decisión  : {args.umbral}")
    print(f"Destino CSV         : {salida_csv}\n")

    print("Cargando modelo Keras (.keras)...")
    modelo_keras = legacy_keras.models.load_model(str(keras_model_path), compile=False)

    print("Cargando modelo TFLite (.tflite)...")
    interpreter = tf.lite.Interpreter(model_path=str(tflite_model_path))
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    extensiones = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
    rutas = [p for p in test_dir.rglob("*") if p.suffix.lower() in extensiones]

    if not rutas:
        print(f"No hay imágenes válidas en la carpeta '{test_dir}'.")
        return

    print(f"\nIniciando análisis de {len(rutas)} imágenes...\n")
    resultados = []

    for ruta in tqdm(rutas, desc="Analizando"):
        img_array = preprocesar_imagen(ruta, img_size=args.img_size)

        prob_keras = modelo_keras.predict(img_array, verbose=0)[0][0]
        clase_keras = "Infestado" if prob_keras >= args.umbral else "Sano"

        interpreter.set_tensor(input_details[0]["index"], img_array)
        interpreter.invoke()
        prob_tflite = interpreter.get_tensor(output_details[0]["index"])[0][0]
        clase_tflite = "Infestado" if prob_tflite >= args.umbral else "Sano"

        resultados.append({
            "Archivo": ruta.name,
            "Keras (%)": round(float(prob_keras) * 100, 2),
            "Clase_Keras": clase_keras,
            "TFLite (%)": round(float(prob_tflite) * 100, 2),
            "Clase_TFLite": clase_tflite,
            "Diferencia": round(abs(float(prob_keras) - float(prob_tflite)) * 100, 4),
        })

    df = pd.DataFrame(resultados)

    print("\n\n=== RESULTADOS DE INFERENCIA ===")
    print(df.to_string(index=False))

    salida_csv.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(salida_csv, index=False)
    print(f"\nAnálisis terminado. Los resultados detallados se guardaron en: {salida_csv}")


if __name__ == "__main__":
    main()
