package calibration;

import static calibration.Helper.PIXELS;
import static calibration.Helper.getImage;

import calibration.obstacle.AbstractObstacle;
import calibration.obstacle.FieldBorder;
import calibration.obstacle.Obstacle;
import calibration.obstacle.ThreatLevel;
import drawer.optimizer.Mesher;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.WritableObjectValue;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.shape.Polygon;
import javax.imageio.ImageIO;
import org.waltonrobotics.motion.Path;

public final class Field {

  private static final Field instance = new Field();
  private static final String SUFFIX = ".field";
  //	public static BufferedImage bufferedImage;
  private static final String MATCH_PATTERN = "[0-9.]+\\s[a-zA-Z]+(?:\\d|\\.|\\s|ERROR|WARNING|M|L|C)*";
  private static final Alert useFieldValue = new Alert(AlertType.CONFIRMATION);

  static {
    useFieldValue
        .setContentText(
            "This file already has field information inside of it do you wish to load it?");


  }

  public final double robotWidth = 0.8171; //TODO make it be set manually
  public final SimpleDoubleProperty SCALE = new SimpleDoubleProperty(1);
  public final WritableObjectValue<String> UNIT = new SimpleStringProperty(PIXELS);
  private final List<AbstractObstacle> fieldObstacles = new ArrayList<>();
  public File imageFile;
  public Image image;
  public Group obstacleGroup = new Group();
  private FieldBorder fieldBorder;

  private Field() {
    Path.setRobotWidth(robotWidth);

//    fieldObstacles.addListener((ListChangeListener<? super AbstractObstacle>) c -> Mesher.createMesh());
  }

  public static Field getInstance() {
    return instance;
  }

  public static boolean isFieldFile(File file) {
    if (file.getName().endsWith(SUFFIX)) {
      Optional<ButtonType> buttonTypeOptional = useFieldValue.showAndWait();

      return buttonTypeOptional.isPresent() && (buttonTypeOptional.get() == ButtonType.OK);
    }

    return false;
  }

  public void clearField() {
    SCALE.set(1.0);
    UNIT.set(Helper.PIXELS);
    fieldObstacles.clear();
    imageFile = null;
    image = null;
    obstacleGroup.getChildren().clear();
    fieldBorder = null;
  }

  public FieldBorder getFieldBorder() {
    return fieldBorder;
  }

  public List<AbstractObstacle> getFieldObstacles() {
    return fieldObstacles;
  }

  public Image loadData(File loadFile) throws IOException {
    clearField();

    try (BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(new FileInputStream(loadFile), StandardCharsets.UTF_8))) {

      if (bufferedReader.readLine().equals("null")) {
        imageFile = null;
        instance.image = null;
      } else {
        Image image = getImage(loadFile);

        imageFile = loadFile;
        instance.image = image;
      }

      AtomicReference<String> lastLine = new AtomicReference<>();

      bufferedReader.lines().forEach(lastLine::set);
      lastLine.set(lastLine.get().trim());

      if (Pattern.matches(MATCH_PATTERN, lastLine.get())) {
        System.out.println("Loading");

        String[] data = lastLine.get().split("\\t");

        SCALE.set(Double.parseDouble(data[0]));
        UNIT.set(data[1]);

        String fieldBorderDataPath = data[2];
        if (!fieldBorderDataPath.isEmpty()) {
          javafx.scene.shape.Path field = Helper.convertStringToPath(fieldBorderDataPath);

          //FIXME makes no sense but since this works oh well
          field.setFill(ThreatLevel.ERROR.getDisplayColor());
          field.setStroke(ThreatLevel.ERROR.getDisplayColor());
          field.setStrokeWidth(1);

          FieldBorder fieldBorder = new FieldBorder(field);
          addObstacle(fieldBorder);
        }

        for (int i = 3; i < data.length; i++) {
          int index = data[i].indexOf(' ');

          String threatLevelName = data[i].substring(0, index);
          String pointData = data[i].substring(index + 1);

          ThreatLevel threatLevel = ThreatLevel.valueOf(threatLevelName);
          Polygon polygon = Helper.loadPolygonFromString(pointData);

          polygon.setFill(threatLevel.getDisplayColor());
          polygon.setStroke(threatLevel.getDisplayColor());
          polygon.setStrokeWidth(1);

          Obstacle obstacle = new Obstacle(threatLevel, polygon);
          addObstacle(obstacle);
        }
      }
    }
    return image;
  }

  public void saveData(File saveFile) throws IOException {
    System.out.println(getFieldBorder().getDefiningShape());

    if (saveFile.exists()) {
      System.out.println((saveFile.delete() ? "M" : "Did not m") + "anaged to delete the file");
    }
    if (saveFile.createNewFile()) {

      if (image != null) {
        String splits = imageFile.getAbsolutePath()
            .substring(imageFile.getAbsolutePath().lastIndexOf('.') + 1);
        ImageIO.write(ImageIO.read(imageFile), "jpg".equals(splits) ? "jpeg" : "png", saveFile);
      }

      try (BufferedWriter bufferedWriter = Files
          .newBufferedWriter(saveFile.toPath(), StandardOpenOption.APPEND)) {
        if (image == null) {
          bufferedWriter.write("null");
        }
        bufferedWriter.newLine();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("%f\t%s", SCALE.get(), UNIT.get()));
        stringBuilder.append('\t');
        if (getFieldBorder() != null) {
          stringBuilder
              .append(Helper.convertPathToString((javafx.scene.shape.Path) getFieldBorder().getDefiningShape()));
        }
        stringBuilder.append('\t');

        fieldObstacles.remove(fieldBorder);
        for (AbstractObstacle abstractObstacle : fieldObstacles) {
          Obstacle obstacle = (Obstacle) abstractObstacle;

          stringBuilder.append(obstacle.getThreatLevel());
          stringBuilder.append(' ');
          stringBuilder.append(Helper.convertPolygonToString((Polygon) obstacle.getDefiningShape()));
          stringBuilder.append('\t');
        }

        fieldObstacles.add(fieldBorder);

        bufferedWriter.write(stringBuilder.toString());
      }
    }
  }

  public void addObstacle(Obstacle obstacle) {
    fieldObstacles.add(obstacle);
    obstacleGroup.getChildren().add(obstacle);

    Mesher.createMesh();
  }

  public void addObstacle(FieldBorder obstacle) {

    if (fieldBorder != null) {

      fieldObstacles.remove(fieldBorder);
      obstacleGroup.getChildren().remove(fieldBorder);
    }

    fieldObstacles.add(obstacle);

    obstacleGroup.getChildren().add(obstacle);
    fieldBorder = obstacle;
    Mesher.createMesh();
  }

  @Override
  public String toString() {
    return "Field{" +
        "robotWidth=" + robotWidth +
        ", SCALE=" + SCALE +
        ", UNIT=" + UNIT +
        ", fieldObstacles=" + fieldObstacles +
        ", imageFile=" + imageFile +
        ", image=" + image +
        ", obstacleGroup=" + obstacleGroup +
        ", fieldBorder=" + fieldBorder +
        '}';
  }

  public void improveImageContrast() {
    ColorAdjust colorAdjust = new ColorAdjust(0, 0, 0, 0);
  }
}