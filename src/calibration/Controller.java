package calibration;

import static calibration.Helper.PIXELS;
import static calibration.Helper.getImage;
import static calibration.Helper.getWindow;

import calibration.Helper.Unit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javax.imageio.ImageIO;

public class Controller implements Initializable {

	public static final String MATCH_PATTERN = "[0-9.]+ [a-zA-Z]+";
	private static final String defaultDistance = "Select 2 Points";
	private static final FileChooser fileChooser = new FileChooser();
	private static final FileChooser saver = new FileChooser();
	private static final FileChooser loader = new FileChooser();
	private static File startLoadLocation;
	private static File startOpenLocation;
	private static File startSaveLocation;

	static {
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Images", "*.png", "*.jpg", "*.field"));
		fileChooser.setTitle("Select Field Image");

		saver.getExtensionFilters().add(new ExtensionFilter("Field data", "*.field"));
		saver.setTitle("Save information");
		loader.getExtensionFilters().add(new ExtensionFilter("Field data", "*.field"));
		loader.setTitle("Load information");

		startOpenLocation = fileChooser.getInitialDirectory();
		startSaveLocation = saver.getInitialDirectory();
		startLoadLocation = saver.getInitialDirectory();
	}

	final TextField convertedInfo = new TextField();
	private final SelectionPoint[] scaleSelectionPoints = new SelectionPoint[2];
	private final Button rescale = new Button("Rescale");
	private final SelectionLine line = new SelectionLine();
	boolean firstConversion = true;
	@FXML
	private ImageView fieldImage;
	@FXML
	private AnchorPane pointPlacement;
	@FXML
	private TextField distanceViewer;
	@FXML
	private HBox infoPane;
	private Selection scaleSelection = Selection.NO_SELECTION;
	private File image;

	public Controller() {
	}

	private static Unit askActualDistance(double pixelDistance) {
		return DistanceConverter.display(pixelDistance);
	}

	private void cleanUp() {
		rescale.setDisable(true);
		line.setVisible(false);
		pointPlacement.getChildren().removeAll(scaleSelectionPoints);
		distanceViewer.setText(defaultDistance);
		convertedInfo.setText(defaultDistance);
		scaleSelection = Selection.NO_SELECTION;
	}

	public final void selectPoint(MouseEvent mouseEvent) {

		if (scaleSelection == Selection.NO_SELECTION) {
			cleanUp();
		}

		SelectionPoint selectionPoint = new SelectionPoint(mouseEvent.getX(), mouseEvent.getY());
		scaleSelectionPoints[scaleSelection.ordinal()] = selectionPoint;
		pointPlacement.getChildren().add(selectionPoint);

		scaleSelection = Selection.values()[scaleSelection.ordinal() + 1];

		if (scaleSelection == Selection.TWO_POINT) {
			rescale.setDisable(false);

			scaleSelection = Selection.NO_SELECTION;

			line.showLine(scaleSelectionPoints[0], scaleSelectionPoints[1], distanceViewer);

			if (firstConversion) {
				setConversion();
			}
		}
	}

	private void setConversion() {
		double pixelDistance = line.getLineLength();
		Unit actualDistance = askActualDistance(pixelDistance);

		if (actualDistance != null) {

			Main.scale.set(actualDistance.getValue() / pixelDistance);
			Main.unit.set(actualDistance.getUnit());

			addExtraData();
			convertedInfo.setText(String.format("%.3f %s", actualDistance.getValue(), Main.unit.get()));
		}
	}

	private void addExtraData() {
		if (firstConversion) {
			firstConversion = false;

			infoPane.getChildren().add(1, new Text("=>"));
			infoPane.getChildren().add(2, convertedInfo);
			infoPane.getChildren().add(infoPane.getChildren().size() - 1, rescale);

		}
	}

	public final void chooseImage(ActionEvent actionEvent) throws java.io.FileNotFoundException {
		fileChooser.setInitialDirectory(startOpenLocation);

		Window window;

		try {
			window = getWindow(actionEvent);
		} catch (ClassCastException e) {
			window = ((MenuItem) actionEvent.getSource()).getParentPopup().getOwnerWindow();
		}

		File image = fileChooser.showOpenDialog(window);

		if (image != null) {
			startOpenLocation = image.getParentFile();

			this.image = image;
			fieldImage.setImage(getImage(image));
			distanceViewer.setText(defaultDistance);
		}
	}

	@Override
	public final void initialize(URL location, ResourceBundle resources) {
		line.setVisible(false);
		pointPlacement.getChildren().add(line);
		convertedInfo.setEditable(false);
		rescale.setOnAction((actionEvent) -> setConversion());
	}

	public final void saveData(ActionEvent actionEvent) throws IOException {
		if (fieldImage != null) {

			saver.setInitialDirectory(startSaveLocation);

			File saveFile = saver
				.showSaveDialog(((MenuItem) actionEvent.getSource()).getParentPopup().getOwnerWindow());

			if (saveFile != null) {
				startSaveLocation = saveFile.getParentFile();

				if (saveFile.exists()) {
					System.out.println((saveFile.delete() ? "M" : "Did not m") + "anage to delete the file");
				}
				if (saveFile.createNewFile()) {

					{
						String splits = image.getAbsolutePath().substring(image.getAbsolutePath().lastIndexOf('.') + 1);
						ImageIO.write(ImageIO.read(image), "jpg".equals(splits) ? "jpeg" : "png", saveFile);
					}

					try (BufferedWriter bufferedWriter = Files
						.newBufferedWriter(saveFile.toPath(), StandardOpenOption.APPEND)) {

						bufferedWriter.newLine();
						bufferedWriter.write(String.format("%f %s", Main.scale.get(), Main.unit.get()));
					}
				}

			}
		}
	}

	public final void loadData(ActionEvent actionEvent) throws IOException {
		loader.setInitialDirectory(startLoadLocation);

		File loadFile = loader.showOpenDialog(((MenuItem) actionEvent.getSource()).getParentPopup().getOwnerWindow());

		if (loadFile != null) {
			startLoadLocation = loadFile.getParentFile();

			Image image = getImage(loadFile);

			fieldImage.setImage(image);

			try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(loadFile)))) {
				AtomicReference<String> lastLine = new AtomicReference<>();

				bufferedReader.lines().forEach(lastLine::set);
				lastLine.set(lastLine.get().trim());

				if (Pattern.matches(MATCH_PATTERN, lastLine.get())) {
					String[] data = lastLine.get().split("\\s");

					System.out.println(Arrays.toString(data));
					Main.scale.set(Double.parseDouble(data[0]));
					Main.unit.set(data[1]);
				} else {
					Main.scale.set(1.0);
					Main.unit.set(PIXELS);
				}

				addExtraData();
				cleanUp();
			}
		}
	}


	public enum Selection {
		NO_SELECTION, ONE_POINT, TWO_POINT
	}

	private static class SelectionPoint extends Circle {

		private final Point2D point2D;

		SelectionPoint(double centerX, double centerY) {
			super(centerX, centerY, 4, Color.BLUE);

			point2D = new Point2D(centerX, centerY);
		}

		final Point2D getPoint2D() {
			return point2D;
		}
	}

	private class SelectionLine extends Line {

		Point2D p1 = new Point2D(0, 0);
		Point2D p2 = new Point2D(0, 0);

		SelectionLine() {
			setFill(Color.RED);
			setStrokeWidth(2);
		}

		final void showLine(SelectionPoint point, SelectionPoint point2, TextField distanceViewer) {
			setVisible(true);

			p1 = point.getPoint2D();
			p2 = point2.getPoint2D();

			setStartX(point.getCenterX());
			setStartY(point.getCenterY());

			setEndX(point2.getCenterX());
			setEndY(point2.getCenterY());

			double distance = getLineLength();

			distanceViewer.setText(String.format("%.4f %s", distance, PIXELS));

			if (!firstConversion) {
				convertedInfo.setText(String.format("%.3f %s", Main.scale.get() * distance, Main.unit.get()));
			}

		}

		final double getLineLength() {
			return p1.distance(p2);
		}
	}
}
