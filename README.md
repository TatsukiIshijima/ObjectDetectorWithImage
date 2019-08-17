# ObjectDetectorWithImage

### 概要
Android 上で学習済みの ssdlite_mobilenet_v2_coco を用いて、画像に対して物体検出を行う

### 開発環境
[![Android Studio](https://img.shields.io/badge/AndroidStudio-3.4.2-blue.svg)](https://developer.android.com/studio/)

### 使用言語
- kotlin

### サポートバージョン
- minSdkVersion 26
- targetSdkVersion 28

### ライブラリ
| 名前 | バージョン | 用途 |
|:-----------|:------------|:------------|
| [Firebase Core](https://firebase.google.com/docs/database/android/start/) | 17.0.1 | Firebase SDK 依存関係 |
| [Firebase ML Model Interpreter](https://firebase.google.com/docs/storage/android/start) | 20.0.1 | TFLiteモデル読み込み |

### 事前準備
本プロジェクトをクローンして使用する場合は以下手順の 2 ~ 5 は不要です。独自のデータセットで Tensorflow Object Detection API などで ssdlite_mobilenet_v2_coco を訓練して使用する場合は2をスキップし、訓練し、Exportした pbファイルを用いて3 ~ 5まで進めてください。

1. 「[Android プロジェクトに Firebase を追加する](https://firebase.google.com/docs/android/setup?hl=ja)」を参考にステップ3-1（Firebase Android 構成ファイルをアプリに追加）まで進める
2. [Tensorflow detection model zoo](https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md) COCOデータセットを用いて学習済みの COCO-trained models から [ssdlite_mobilenet_v2_coco](http://download.tensorflow.org/models/object_detection/ssdlite_mobilenet_v2_coco_2018_05_09.tar.gz) をダウンロード
3. Python の Tensorflow をインストールし、`tflite_convert` コマンドが使用できるようにしておく
4. 2.でダウンロードしたものを解凍し、内部にある `frozen_inference_graph.pb` を以下コマンドの `graph_def_file` に指定して以下を実行し、tfliteファイルへ変換
```
tflite_convert --graph_def_file=frozen_inference_graph.pb
               --output_file=ssdlite_mobilenet_v2_coco.tflite
               --inference_type=FLOAT
               --input_shape=1,300,300,3
               --input_array=Preprocessor/sub
               --output_arrays=concat,concat_1
```
5. AndroidProject の app -> src -> main -> assets 配下に 4. で変換した tfliteファイルを配置する

### 実行
1. AndroidProject の app -> src -> main -> res -> drawable 配下に任意の画像を配置する
2. MainActivity #50 に 1. で配置した画像を指定する

### 画面
