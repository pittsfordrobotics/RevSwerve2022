package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.Paths;
import frc.robot.util.TimeKeeper;

public class Auto5Ball extends SequentialCommandGroup {
    public Auto5Ball() {
        super(
                new TimeKeeper(true),
                new SwerveResetPose(Paths.FIVE_BALL.get(0), true),
                new ParallelDeadlineGroup(
                        new WaitCommand(1),
                        new InstantCommand(() -> System.out.println("Shoot Ball"))
                ),
                new InstantCommand(() -> System.out.println("Intake Down")),
                new SwervePathing(Paths.FIVE_BALL.get(0), false),
                new ParallelDeadlineGroup(
                        new WaitCommand(1),
                        new InstantCommand(() -> System.out.println("Wait for Ball"))
                ),
                new SwervePathing(Paths.FIVE_BALL.get(1), false),
                new ParallelDeadlineGroup(
                        new WaitCommand(1),
                        new InstantCommand(() -> System.out.println("Wait for Ball"))
                ),
                new SwervePathing(Paths.FIVE_BALL.get(2), false),
                new InstantCommand(() -> System.out.println("Intake Up")),
                new ParallelDeadlineGroup(
                        new WaitCommand(1),
                        new InstantCommand(() -> System.out.println("Align and Shoot Ball"))
                ),
                new InstantCommand(() -> System.out.println("Intake Down")),
                new SwervePathing(Paths.FIVE_BALL.get(3), false),
                new ParallelDeadlineGroup(
                        new WaitCommand(1),
                        new InstantCommand(() -> System.out.println("Detecting Ball"))
                ),
                new SwervePathing(Paths.FIVE_BALL.get(4), false),
                new ParallelDeadlineGroup(
                        new WaitCommand(1),
                        new InstantCommand(() -> System.out.println("Wait for Ball"))
                ),
                new SwervePathing(Paths.FIVE_BALL.get(5), false),
                new ParallelDeadlineGroup(
                        new WaitCommand(1),
                        new InstantCommand(() -> System.out.println("Align and shoot"))
                ),
                new TimeKeeper(false)
        );
    }
}