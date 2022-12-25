package frc.robot.subsystems.swerve;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.util.*;
import frc.robot.util.Alert.AlertType;
import org.littletonrobotics.junction.Logger;

public class Swerve extends SubsystemBase {
    /*
     * Swerve Module Orientation
     *        FL  FR
     *
     *        BL  BR
     */
    private final SwerveModuleIO[] moduleIO;
    private final SwerveModuleIOInputsAutoLogged[] moduleInputs = new SwerveModuleIOInputsAutoLogged[] {new SwerveModuleIOInputsAutoLogged(), new SwerveModuleIOInputsAutoLogged(), new SwerveModuleIOInputsAutoLogged(), new SwerveModuleIOInputsAutoLogged()};

    public enum Modules {
        FRONT_LEFT(0), FRONT_RIGHT(1), BOTTOM_LEFT(2), BOTTOM_RIGHT(3);

        final int id;
        Modules(int id){
            this.id = id;
        }
        public int getId() {
            return id;
        }
    }

    private final GyroIO gyroIO;
    private final GyroIOInputsAutoLogged gyroInputs = new GyroIOInputsAutoLogged();

    private final SwerveDriveKinematics driveKinematics = new SwerveDriveKinematics(Constants.SWERVE_MODULE_OFFSETS);
    private final BetterSwerveKinematics driveKinematicsBetter = new BetterSwerveKinematics(Constants.SWERVE_MODULE_OFFSETS);
    private final SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];
    private SwerveModuleState[] moduleStates = new SwerveModuleState[]{new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState()};
    private ChassisSpeeds chassisSpeeds = new ChassisSpeeds();
    private final SwerveDrivePoseEstimator poseEstimator;

    private Rotation2d lastRotation = new Rotation2d();

    private final PIDController xPIDController = new PIDController(0,0,0);
    private final PIDController yPIDController = new PIDController(0,0,0);

    private final Alert pigeonAlert = new Alert("Pigeon not detected! Falling back to estimated angle!", AlertType.ERROR);

    private final static Swerve INSTANCE = new Swerve(Constants.ROBOT_FL_SWERVE_MODULE, Constants.ROBOT_FR_SWERVE_MODULE, Constants.ROBOT_BL_SWERVE_MODULE, Constants.ROBOT_BR_SWERVE_MODULE, Constants.ROBOT_GYRO);

    public static Swerve getInstance() {
        return INSTANCE;
    }

    private Swerve(SwerveModuleIO FL, SwerveModuleIO FR, SwerveModuleIO BL, SwerveModuleIO BR, GyroIO gyro) {
        moduleIO = new SwerveModuleIO[]{FL, FR, BL, BR};
        gyroIO = gyro;

        for (int i = 0; i < 4; i++) {
            moduleIO[i].updateInputs(moduleInputs[i]);
        }
        for (int i = 0; i < 4; i++) {
//            this uses relative steer encoder, this could also use absolute if needed
            modulePositions[i] = new SwerveModulePosition(moduleInputs[i].drivePositionMeters, Rotation2d.fromRadians(moduleInputs[i].steerPositionRad));
        }

        poseEstimator = new SwerveDrivePoseEstimator(driveKinematics, new Rotation2d(), modulePositions, new Pose2d());
    }

    @Override
    public void periodic() {
        SwerveModuleState[] actualStates = new SwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            moduleIO[i].updateInputs(moduleInputs[i]);
            actualStates[i] = new SwerveModuleState(moduleInputs[i].driveVelocityMetersPerSec, Rotation2d.fromRadians(moduleInputs[i].steerPositionRad));
        }
        chassisSpeeds = driveKinematics.toChassisSpeeds(actualStates);

        Logger.getInstance().processInputs("FL Swerve Module", moduleInputs[0]);
        Logger.getInstance().processInputs("FR Swerve Module", moduleInputs[1]);
        Logger.getInstance().processInputs("BL Swerve Module", moduleInputs[2]);
        Logger.getInstance().processInputs("BR Swerve Module", moduleInputs[3]);

        gyroIO.updateInputs(gyroInputs);
        Logger.getInstance().processInputs("Gyro", gyroInputs);

        for (int i = 0; i < 4; i++) {
            modulePositions[i] = new SwerveModulePosition(moduleInputs[i].drivePositionMeters, Rotation2d.fromRadians(moduleInputs[i].steerPositionRad));
        }
        lastRotation = getRotation();
        poseEstimator.update(getRotation(), modulePositions);

        Logger.getInstance().recordOutput("Swerve/Pose", getPose());
        Logger.getInstance().recordOutput("Swerve/Wanted States", moduleStates);
        Logger.getInstance().recordOutput("Swerve/Actual States", actualStates);
        Logger.getInstance().recordOutput("Swerve/Chassis Speeds X", chassisSpeeds.vxMetersPerSecond);
        Logger.getInstance().recordOutput("Swerve/Chassis Speeds Y", chassisSpeeds.vyMetersPerSecond);
        Logger.getInstance().recordOutput("Swerve/Chassis Speeds Rot", chassisSpeeds.omegaRadiansPerSecond);

        pigeonAlert.set(!gyroInputs.connected);
    }

    public void setModuleStates(BetterSwerveModuleState[] desiredModuleStates) {
        BetterSwerveModuleState[] moduleStatesOptimized = new BetterSwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            moduleStatesOptimized[i] = SwerveOptimizer.optimize(desiredModuleStates[i], modulePositions[i].angle);
        }
        BetterSwerveKinematics.desaturateWheelSpeeds(moduleStatesOptimized, Constants.SWERVE_MAX_MODULE_VELOCITY_METERS_PER_SECOND);
        for (int i = 0; i < 4; i++) {
            moduleIO[i].setModuleState(moduleStatesOptimized[i]);
        }
        moduleStates = moduleStatesOptimized;
    }

    public void setModuleStates(ChassisSpeeds speeds) {
        setModuleStates(driveKinematicsBetter.toSwerveModuleStates(speeds));
    }

    public void driveFieldOrientated(double vxMetersPerSecond, double vyMetersPerSecond, double omegaRadiansPerSecond) {
        setModuleStates(ChassisSpeeds.fromFieldRelativeSpeeds(vxMetersPerSecond, vyMetersPerSecond, omegaRadiansPerSecond, getPose().getRotation()));
    }

//    drives wheels at x to prevent being shoved
    public void driveX() {
        setModuleStates(new BetterSwerveModuleState[]{
                new BetterSwerveModuleState(0.1, Rotation2d.fromDegrees(-45), 0),
                new BetterSwerveModuleState(0.1, Rotation2d.fromDegrees(225), 0),
                new BetterSwerveModuleState(0.1, Rotation2d.fromDegrees(45), 0),
                new BetterSwerveModuleState(0.1, Rotation2d.fromDegrees(135), 0),
        });
    }

    public void driveZero() {
        setModuleStates(new BetterSwerveModuleState[]{
                new BetterSwerveModuleState(0, new Rotation2d(0),0),
                new BetterSwerveModuleState(0, new Rotation2d(0),0),
                new BetterSwerveModuleState(0, new Rotation2d(0),0),
                new BetterSwerveModuleState(0, new Rotation2d(0),0)
        });
    }

    public void resetPose(Pose2d pose) {
        poseEstimator.resetPosition(getRotation(), modulePositions, pose);
    }

    public void addVisionData(Pose2d pose, double time) {
//        this is recommended, but I'm not sure if I like it
//        if (GeomUtil.distance(pose, getPose()) < 1) {
            poseEstimator.addVisionMeasurement(pose, time);
//        }
    }

    public SwerveDriveKinematics getKinematics() {
        return driveKinematics;
    }

    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }

    /**
     * Gets the pigeon's angle
     * @return current angle; positive = clockwise
     */
    public Rotation2d getRotation() {
        if (gyroInputs.connected) {
            return Rotation2d.fromRadians(-gyroInputs.yawPositionRad);
        }
        else {
            return Rotation2d.fromRadians(chassisSpeeds.omegaRadiansPerSecond * Constants.ROBOT_LOOP_TIME_SECONDS + lastRotation.getRadians());
        }
    }
}