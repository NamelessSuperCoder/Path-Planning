package drawer.curves;

import calibration.Field;
import drawer.curves.figures.OriginPoint;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleDoubleProperty;
import org.waltonrobotics.controller.Pose;

public class PointAngleGroup extends PointGroup {

  private static int index;
  private final SimpleDoubleProperty translatedX = new SimpleDoubleProperty(1.0);
  private final SimpleDoubleProperty translatedY = new SimpleDoubleProperty(1.0);
  private final SimpleDoubleProperty translatedAngle = new SimpleDoubleProperty(1.0);
  private OriginPoint originPoint;

  public PointAngleGroup(double centerX, double centerY) {
    super(centerX, centerY);

    setName(String.format("Point %d", index++));
  }

  public PointAngleGroup(Pose pose) {
    super(pose.getX(), pose.getY());

    angleProperty().set(pose.getAngle());
    setName(String.format("Point %d", index++));
  }

  public static List<Pose> mapToPoses(Collection<? extends PointAngleGroup> points) {
    return points.stream().map(PointAngleGroup::getPose).collect(Collectors.toList());
  }

  public static List<Pose> mapToRealPoses(Collection<? extends PointAngleGroup> keyPoints) {
    return keyPoints.stream().map(PointAngleGroup::getRealPose).collect(Collectors.toList());
  }

  public PointGroup getOriginPoint() {
    return originPoint;
  }

  public SimpleDoubleProperty translatedXProperty() {
    return translatedX;
  }

  public SimpleDoubleProperty translatedYProperty() {
    return translatedY;
  }


  public SimpleDoubleProperty translatedAngleProperty() {
    return translatedAngle;
  }

  private Pose getRealPose() {
    return new Pose(translatedX.get(), translatedY.get(),
        -StrictMath.toRadians(translatedAngle.get()));
  }


  public Pose getPose() {
    return new Pose(getPositionPoint().getCenterX(), getPositionPoint().getCenterY(),
        -angleProperty().get());
  }

  public void setOrigin(OriginPoint originPoint) {
//TODO make setting the origin better
    this.originPoint = originPoint;

    translatedX.unbind();
    translatedX
        .bind(getPositionPoint().centerXProperty().subtract(originPoint.centerXProperty())
            .multiply(Field.getInstance().SCALE));

    translatedY.unbind();
    translatedY
        .bind(getPositionPoint().centerYProperty().subtract(originPoint.centerYProperty())
            .multiply(Field.getInstance().SCALE));

    translatedAngle.unbind();
    translatedAngle.bind(angleProperty().subtract(originPoint.angleProperty()));

    DoubleBinding angleDegrees = Bindings
        .createDoubleBinding(() -> boundDegrees(StrictMath.toDegrees(translatedAngle.get())),
            translatedAngle);

    degreesProperty().unbind();
    degreesProperty().bind(angleDegrees);
  }

  @Override
  public SimpleDoubleProperty degreesProperty() {
    return super.degreesProperty();
  }
}
