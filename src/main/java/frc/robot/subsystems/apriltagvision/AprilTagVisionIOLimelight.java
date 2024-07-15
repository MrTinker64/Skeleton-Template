package frc.robot.subsystems.apriltagvision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.DoubleArraySubscriber;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.TimestampedDoubleArray;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.util.Alert;
import frc.robot.util.LimelightHelpers;
import frc.robot.util.PoseManager;

public class AprilTagVisionIOLimelight implements AprilTagVisionIO {
  private String name;
  private final PoseManager poseManager;

  private static final double disconnectedTimeout = 0.5;
  private final Alert disconnectedAlert;

  public AprilTagVisionIOLimelight(String camName, PoseManager poseManager) {
    name = camName;
    this.poseManager = poseManager;

    LimelightHelpers.setLEDMode_PipelineControl(name);

    var topic =
        LimelightHelpers.getLimelightNTTable("limelight")
            .getDoubleArrayTopic("botpose_orb_wpiblue");
    observationSubscriber =
        topic.subscribe(
            new double[] {}, PubSubOption.keepDuplicates(true), PubSubOption.sendAll(true));

    disconnectedAlert = new Alert("No data from: " + name, Alert.AlertType.ERROR);
  }

  @Override
  public void updateInputs(AprilTagVisionIOInputs inputs) {
    LimelightHelpers.SetRobotOrientation(
        "limelight", poseManager.getRotation().getDegrees(), 0, 0, 0, 0, 0);
    LimelightHelpers.PoseEstimate observation = LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2("limelight");

    inputs.estimatedPose = observation.pose;
    inputs.isNew = observation.timestampSeconds != 0;
    if (inputs.isNew) {
      inputs.timestamp = observation.timestampSeconds;
    }
    inputs.tagCount = observation.tagCount;

    inputs.pipeline = LimelightHelpers.getCurrentPipelineIndex(name);
    inputs.ledMode = LimelightHelpers.getLimelightNTDouble(name, "ledMode");

    // Update disconnected alert
    disconnectedAlert.set(Timer.getFPGATimestamp() - observation.timestampSeconds < disconnectedTimeout);
  }

  @Override
  public void setPipeline(int pipelineIndex) {
    LimelightHelpers.setPipelineIndex(name, pipelineIndex);
  }
}
