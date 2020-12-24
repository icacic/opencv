package hr.ericsson.etkc.ebusiness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import hr.ericsson.etkc.ebusiness.utils.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OpenCVController {

	@FXML
	private Button button;
	@FXML
	private ImageView currentFrame;

	private ScheduledExecutorService timer;
	private VideoCapture capture = new VideoCapture(0);
	private boolean cameraActive = false;
	private static int cameraId = 0;

	/**
	 * The action triggered by pushing the button on the GUI
	 *
	 * @param event the push button event
	 */
	@FXML
	protected void startCamera(ActionEvent event) {
		if (!this.cameraActive) {
			this.capture.open(cameraId);

			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					@Override
					public void run() {
						Mat frame = grabFrame();
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(currentFrame, imageToShow);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				this.button.setText("Stop Camera");
			} else {
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			this.cameraActive = false;
			this.button.setText("Start Camera");
			this.stopAcquisition();
		}
	}

	private Mat grabFrame() {
		Mat frame = new Mat();
		if (this.capture.isOpened()) {
			try {
				this.capture.read(frame);
				if (!frame.empty()) {
					frame = detectFace(frame);
				}
			} catch (Exception e) {
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		return frame;
	}

	public Image getCaptureWithFaceDetection() {
		Mat mat = new Mat();
		this.capture.read(mat);
		//Mat haarClassifiedImg = detectFace(mat);
		return mat2Img(mat);
	}

	private Mat detectFace(Mat mat) {
		try {
			CascadeClassifier cascadeClassifier = new CascadeClassifier("C:\\dev\\opencv\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_alt.xml");
			MatOfRect facesDetected = new MatOfRect();
			int minFaceSize = Math.round(mat.rows() * 0.1f);
			cascadeClassifier.load("C:\\dev\\opencv\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_alt.xml");
			cascadeClassifier.detectMultiScale(mat, facesDetected, 1.1, 3, Objdetect.CASCADE_SCALE_IMAGE,
					new Size(minFaceSize, minFaceSize), new Size());
			Rect[] facesArray = facesDetected.toArray();
			for (Rect face : facesArray) {
				Imgproc.rectangle(mat, face.tl(), face.br(), new Scalar(0, 0, 255), 3);
			}

			return mat;
		} catch (Exception e) {
			e.printStackTrace();
			return mat;
		}
		

	}

	public Image mat2Img(Mat mat) {
		MatOfByte bytes = new MatOfByte();
		Imgcodecs.imencode("img", mat, bytes);
		InputStream inputStream = new ByteArrayInputStream(bytes.toArray());
		return new Image(inputStream);
	}

	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened()) {
			this.capture.release();
		}
	}

	private void updateImageView(ImageView view, Image image) {
		Utils.onFXThread(view.imageProperty(), image);
	}

	protected void setClosed() {
		this.stopAcquisition();
	}

}
