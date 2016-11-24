package org.firstinspires.ftc.teamcode;

import android.graphics.Bitmap;
import android.widget.TextView;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;

import ftclib.FtcChoiceMenu;
import ftclib.FtcMenu;
import ftclib.FtcOpMode;
import ftclib.FtcValueMenu;
import hallib.HalDashboard;

//import com.qualcomm.robotcore.eventloop.opmode.Disabled;

@SuppressWarnings({"unused", "ForLoopReplaceableByForEach"})
@Autonomous(name="AutonTest", group="Auton")
//@Disabled
public class FtcAutoTest extends FtcOpMode implements FtcMenu.MenuButtons
{
    public FtcAutoTest()
    {
        super();
    }

    @Override
    public void initRobot()
    {
        telemetry.addData("_","PLEASE WAIT - STARTING");
        telemetry.update();
        dashboard = getDashboard();
        FtcRobotControllerActivity act = (FtcRobotControllerActivity)(hardwareMap.appContext);
        dashboard.setTextView((TextView)act.findViewById(R.id.textOpMode));
        setup();
    }

    @Override
    public void startMode()
    {
        dashboard.clearDisplay();
        robot.gyro.resetZAxisIntegrator();

        //test max speed
        double lavgspd = 0.0;
        double ravgspd = 0.0;
        double s1avgspd = 0.0;
        double s2avgspd = 0.0;
        int lcnts = 0;
        int rcnts = 0;
        int lcnts_last = 0;
        int rcnts_last = 0;
        int s1cnts = 0;
        int s2cnts = 0;
        int s1cnts_last = 0;
        int s2cnts_last = 0;
        double dur = 0.1;
        double testTimeout = 5;
        robot.leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        robot.rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        robot.leftMotor.setPower(1.0);
        robot.rightMotor.setPower(1.0);
        robot.shotmotor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.shotmotor2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.shotmotor1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        robot.shotmotor2.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        robot.shotmotor1.setPower(1.0);
        robot.shotmotor2.setPower(1.0);
        ElapsedTime sTimer = new ElapsedTime();
        ElapsedTime mspdTimer = new ElapsedTime();
        while(mspdTimer.seconds() < testTimeout)
        {
            double elapsed = sTimer.seconds();
            if(elapsed >= dur)
            {
                lcnts = robot.leftMotor.getCurrentPosition();
                rcnts = robot.rightMotor.getCurrentPosition();
                lavgspd = (lcnts - lcnts_last)/elapsed;
                ravgspd = (rcnts - rcnts_last)/elapsed;
                lcnts_last = lcnts;
                rcnts_last = rcnts;

                s1cnts = robot.shotmotor1.getCurrentPosition();
                s2cnts = robot.shotmotor2.getCurrentPosition();
                s1avgspd = (s1cnts - s1cnts_last)/elapsed;
                s2avgspd = (s2cnts - s2cnts_last)/elapsed;
                s1cnts_last = s1cnts;
                s2cnts_last = s2cnts;

                DbgLog.msg("SJH: %4.2f LEFT  AVG SPD %4.2f", mspdTimer.seconds(), lavgspd);
                DbgLog.msg("SJH: %4.2f RIGHT AVG SPD %4.2f", mspdTimer.seconds(), ravgspd);
                DbgLog.msg("SJH: %4.2f SHOT1 AVG SPD %4.2f", mspdTimer.seconds(), s1avgspd);
                DbgLog.msg("SJH: %4.2f SHOT2 AVG SPD %4.2f", mspdTimer.seconds(), s2avgspd);

                sTimer.reset();
            }
        }

        do_main_loop();
    }

    @Override
    public void runPeriodic(double elapsedTime)
    {
        dashboard.displayPrintf(6, "GHDG: %d",
                robot.gyro.getIntegratedZValue());
    }

    @Override
    public void stopMode()
    {
        drvTrn.stopAndReset();
    }

    private void setup()
    {
        dashboard.displayPrintf(2, "STATE: %s", "INITIALIZING - PLEASE WAIT FOR MENU");
        hardwareMap.logDevices();
        robot.init(hardwareMap);

        int lms = robot.leftMotor.getMaxSpeed();
        int rms = robot.rightMotor.getMaxSpeed();
        DbgLog.msg("SJH: MaxSpeeds %d %d", lms, rms);

        tracker = new ImageTracker();

        doMenus();

        if(team == Team.SNOWMAN)
        {
            DEF_SHT_PWR = SHT_PWR_SNOWMAN;
        }
        else
        {
            DEF_SHT_PWR = SHT_PWR_SONIC;
        }

        if (robot.leftMotor  != null &&
            robot.rightMotor != null &&
            robot.gyro       != null)
        {
            drvTrn.init(robot.leftMotor, robot.rightMotor, robot.gyro);
            drvTrn.setOpMode(getInstance());
            robot.gyro.calibrate();

            // make sure the gyro is calibrated before continuing
            while (!isStopRequested() && robot.gyro.isCalibrating())
            {
                sleep(50);
                idle();
            }
        }


        pathSegs = new Segment[1];

        Segment tseg = new Segment("TESTSEG", new Point2d(0.0, 0.0), new Point2d(0.0, testDist)); 
        tseg.setSpeed(testSpeed);
        drvTrn.setDrvTuner(testK);

        pathSegs[0] = tseg;

        initHdg = pathSegs[0].getFieldHeading();

        Point2d currPoint = pathSegs[0].getStrtPt();
        drvTrn.setCurrPt(currPoint);
        drvTrn.setInitHdg(initHdg);

        robot.pusher.setPosition(ZER_PUSH_POS);

        timer.reset();
        DbgLog.msg("SJH Start %s. Time: %6.3f", currPoint, timer.time());

        dashboard.displayPrintf(3, "PATH: Start at %s %6.3f", currPoint,
                                                              timer.seconds());

        dashboard.displayPrintf(6, "GHDG: %d",
                robot.gyro.getIntegratedZValue());
    }

    private void do_main_loop()
    {
        boolean SkipNextSegment = false;
        for (int i = 0; i < pathSegs.length; ++i)
        {
            if(!opModeIsActive()) break;
            if (SkipNextSegment)
            {
                SkipNextSegment = false;
                continue;
            }

            Segment curSeg;
            curPos = null; //SBH REMOVE
            if(curPos == null)
            {
                curSeg = pathSegs[i];
            }
            else
            {
                drvTrn.setCurrPt(curPos);
                curSeg = new Segment("CURSEG", curPos, pathSegs[i].getTgtPt());
                curPos = null;
            }

            doEncoderTurn(curSeg); //quick but rough
            doTurn(curSeg); //fine tune using gyro
            doMove(curSeg);

            if(curSeg.getPostTurn() != null)
            {
                doEncoderTurn(pathSegs[i+1]);
                doTurn(pathSegs[i+1]);
            }

            DbgLog.msg("SJH Planned pos: %s %s",
                    pathSegs[i].getTgtPt(),
                    pathSegs[i].getFieldHeading());

            switch(pathSegs[i].getName())
            {
                case "START_PT":
                    break;
            }
            switch (pathSegs[i].getAction())
            {
                case SHOOT:
                    do_shoot();
                    break;
                case SCAN_IMAGE:
                    if (findSensedLoc())
                    {
                        DbgLog.msg("SJH Sensed pos: %s %s",
                                curPos, curHdg);
                    }
                    break;
                case FIND_BEACON:

                    SkipNextSegment = !do_findBeaconOrder(true);

                    break;
                case RST_PUSHER:
                    robot.pusher.setPosition(ZER_PUSH_POS);
                    break;
                case NOTHING:
                    break;
            }
        }
    }

    private void doMove(Segment seg)
    {
        String  snm = seg.getName();
        Point2d spt = seg.getStrtPt();
        Point2d ept = seg.getTgtPt();
        double  fhd = seg.getFieldHeading();
        Segment.SegDir dir = seg.getDir();
        double speed = seg.getSpeed();

        RobotLog.ii("SJH", "Drive %s %s %s %6.2f %3.2f %s",
                snm, spt, ept, fhd, speed, dir);

        dashboard.displayPrintf(2, "STATE: %s %s %s - %s %6.2f %3.2f %s",
                "DRIVE", snm, spt, ept, fhd, speed, dir);

        Drivetrain.Direction ddir = Drivetrain.Direction.FORWARD;
        if (dir == Segment.SegDir.REVERSE) ddir = Drivetrain.Direction.REVERSE;
        timer.reset();
        Point2d pt = seg.getTgtPt();
        drvTrn.driveToPointLinear(pt, speed, ddir);
        RobotLog.ii("SJH", "Completed move %s. Time: %6.3f HDG: %5.2f",
                seg.getName(), timer.time(), getGryoFhdg());
    }

    private double getGryoFhdg()
    {
        double cHdg = robot.gyro.getIntegratedZValue() + initHdg;

        while (cHdg <= -180.0) cHdg += 360.0;
        while (cHdg >   180.0) cHdg -= 360.0;

        return cHdg;
    }

    private void doEncoderTurn(Segment seg)
    {
        if (seg.getDir() == Segment.SegDir.REVERSE) return;
        double cHdg = getGryoFhdg();
        double tHdg = seg.getFieldHeading();
        double angle = tHdg - cHdg;
        DbgLog.msg("SJH: doEncoderTurn %s CHDG %4.1f THDG %4.1f",
                seg.getName(),
                cHdg,
                tHdg);

        while (angle <= -180.0) angle += 360.0;
        while (angle >   180.0) angle -= 360.0;
        if(Math.abs(angle) <= 1.0) return;

        DbgLog.msg("SJH: Turn %5.2f", angle);
        dashboard.displayPrintf(2, "STATE: %s %5.2f", "TURN", angle);
        timer.reset();
        drvTrn.ctrTurnLinear(angle,DEF_TRN_PWR);
        //drvTrn.ctrTurnLinearGyro(angle,DEF_TRN_PWR);
        cHdg = getGryoFhdg();
        DbgLog.msg("SJH Completed turn %5.2f. Time: %6.3f CHDG: %5.2f",
                angle, timer.time(), cHdg);
    }

    private void doTurn(Segment seg)
    {
        double tHdg = seg.getFieldHeading();
        if(seg.getDir() == Segment.SegDir.REVERSE)
            return;
        double cHdg = getGryoFhdg();

        DbgLog.msg("SJH: doGyroTurn %s CHDG %4.1f THDG %4.1f",
                seg.getName(),
                cHdg,
                tHdg);

        if(Math.abs(tHdg-cHdg) <= 1.0)
            return;

        timer.reset();
        drvTrn.ctrTurnToHeading(tHdg, DEF_TRN_PWR);
        cHdg = getGryoFhdg();
        DbgLog.msg("SJH Completed turnGyro %5.2f. Time: %6.3f CHDG: %5.2f",
                tHdg, timer.time(), cHdg);
    }

    private boolean findSensedLoc()
    {
        DbgLog.msg("SJH findSensedLoc");
        dashboard.displayPrintf(2, "STATE: %s", "FIND IMG LOC");
        curPos = null;
        ElapsedTime itimer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        tracker.setActive(true);
        Point2d sensedBotPos = null;
        double  sensedFldHdg = pathSegs[0].getFieldHeading();
        while(sensedBotPos == null && itimer.milliseconds() < 1000)
        {
            tracker.updateRobotLocationInfo();
            sensedBotPos = tracker.getSensedPosition();

            if(sensedBotPos != null)
            {
                curPos = sensedBotPos;
                sensedFldHdg = tracker.getSensedFldHeading();
                curHdg = sensedFldHdg;
            }
            sleep(50);
        }

        tracker.setActive(false);

        if ( sensedBotPos != null )
        {
            DbgLog.msg("Image based location: %s %5.2f", sensedBotPos, sensedFldHdg);
            dashboard.displayPrintf(4, "SENSLOC: %s %5.2f",
                    sensedBotPos, sensedFldHdg);
        }
        else
        {
            dashboard.displayPrintf(4, "SENSLOC: %s", "NO VALUE");
        }

        return (curPos != null);
    }

    private void do_beacon_push_integrated()
    {
        RobotLog.ii("SJH", "FIND/PUSH");
        dashboard.displayPrintf(2, "STATE: %s", "BEACON FIND/PUSH");
        do_findBeaconOrder(true);
    }

    private boolean do_findBeaconOrder(boolean push)
    {
        DbgLog.msg("SJH: FIND BEACON ORDER!!!");
        dashboard.displayPrintf(2, "STATE: %s", "BEACON FIND");
        int timeout = 1000;
        BeaconFinder.LightOrder ord = BeaconFinder.LightOrder.UNKNOWN;
        ElapsedTime itimer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);

        tracker.setFrameQueueSize(10);
        tracker.setActive(true);
        while  ((ord == BeaconFinder.LightOrder.UNKNOWN ||
                ord == BeaconFinder.LightOrder.RED_RED ||
                ord == BeaconFinder.LightOrder.BLUE_BLUE) &&
               itimer.milliseconds() < timeout)
        {
            Bitmap bmap = tracker.getImage();
            if(bmap != null)
            {
                bd.setBitmap(bmap);
                ord = bd.getLightOrder();
            }
            sleep(50);
        }
        tracker.setActive(false);
        tracker.setFrameQueueSize(0);

        if (ord != BeaconFinder.LightOrder.UNKNOWN)
        {
            DbgLog.msg("SJH: Found Beacon!!! " + ord);
        }

        if (ord == BeaconFinder.LightOrder.BLUE_RED)
        {
            if (alliance == Field.Alliance.BLUE)
            {
                bSide = ButtonSide.LEFT;
            }
            else
            {
                bSide = ButtonSide.RIGHT;
            }
        }
        else if (ord == BeaconFinder.LightOrder.RED_BLUE)
        {
            if (alliance == Field.Alliance.BLUE)
            {
                bSide = ButtonSide.RIGHT;
            }
            else
            {
                bSide = ButtonSide.LEFT;
            }
        }

        DbgLog.msg("SJH: Gonna push button " + bSide);
        dashboard.displayPrintf(5, "BUTTON: %s", bSide);

        if(push)
        {
            do_pushButton(bSide);
        }
        return bSide != ButtonSide.UNKNOWN;
    }

    private void do_pushButton(ButtonSide bside)
    {
        DbgLog.msg("SJH: PUSH BUTTON!!!");
        dashboard.displayPrintf(2, "STATE: %s", "BUTTON PUSH");
        if (bside == ButtonSide.LEFT)
        {
            robot.pusher.setPosition(LFT_PUSH_POS);
            DbgLog.msg("SJH: Pushing left button");
        }
        else if (bside == ButtonSide.RIGHT)
        {
            robot.pusher.setPosition(RGT_PUSH_POS);
            DbgLog.msg("SJH: Pushing right button");
        }
        else
        {
            robot.pusher.setPosition(CTR_PUSH_POS);
            DbgLog.msg("SJH: Not Pushing A Button");

        }

    }

    private void do_shoot()
    {
        DbgLog.msg("SJH: SHOOT!!!");
        dashboard.displayPrintf(2, "STATE: %s", "SHOOT");
        ElapsedTime stimer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        robot.shotmotor1.setPower(DEF_SHT_PWR);
        robot.shotmotor2.setPower(DEF_SHT_PWR);
        sleep(500);
        robot.sweepMotor.setPower(-DEF_SWP_PWR);
        robot.elevMotor.setPower(-DEF_ELV_PWR);
        sleep(1500);
        robot.shotmotor1.setPower(0);
        robot.shotmotor2.setPower(0);
        robot.sweepMotor.setPower(0);
        robot.elevMotor.setPower(0);
    }

    //
    // Implements FtcMenu.MenuButtons interface.
    //

    @Override
    public boolean isMenuUpButton() { return gamepad1.dpad_up;} //isMenuUpButton

    @Override
    public boolean isMenuDownButton()
    {
        return gamepad1.dpad_down;
    } //isMenuDownButton

    @Override
    public boolean isMenuEnterButton()
    {
        return gamepad1.a;
    } //isMenuEnterButton

    @Override
    public boolean isMenuBackButton()
    {
        return gamepad1.dpad_left;
    }  //isMenuBackButton

    private void doMenus()
    {
        //
        // Create the menus.
        //
        FtcChoiceMenu startPosMenu = new FtcChoiceMenu("START:", null, this);
        FtcChoiceMenu pushMenu     = new FtcChoiceMenu("PUSH:", startPosMenu, this);
        FtcChoiceMenu parkMenu     = new FtcChoiceMenu("PARK:", pushMenu, this);
        FtcChoiceMenu allianceMenu = new FtcChoiceMenu("ALLIANCE:", parkMenu, this);
        FtcChoiceMenu teamMenu     = new FtcChoiceMenu("TEAM:", allianceMenu, this);
        FtcValueMenu  testDistMenu = new FtcValueMenu("DIST:",  startPosMenu, this, 0.0, 60.0, 1.0, 48.0,  "%4.1f");
        FtcValueMenu  testSpdMenu  = new FtcValueMenu("SPEED:", testDistMenu, this, 0.0, 1.0, 0.1, 0.5,  "%4.1f");
        FtcValueMenu  testKMenu    = new FtcValueMenu("K:",     testSpdMenu,  this, 0.5, 1.5, 0.01, 1.0, "%4.2f");

        startPosMenu.addChoice("Start_A", Field.StartPos.START_A, pushMenu);
        startPosMenu.addChoice("Start_B", Field.StartPos.START_B, pushMenu);
        startPosMenu.addChoice("Start_TEST", Field.StartPos.START_TEST, testDistMenu);
        pushMenu.addChoice("BOTH", Field.BeaconChoice.BOTH, parkMenu);
        pushMenu.addChoice("NEAR", Field.BeaconChoice.NEAR, parkMenu);
        pushMenu.addChoice("FAR",  Field.BeaconChoice.FAR,  parkMenu);
        pushMenu.addChoice("NONE", Field.BeaconChoice.NONE, parkMenu);

        parkMenu.addChoice("CENTER", Field.ParkChoice.CENTER_PARK, allianceMenu);
        parkMenu.addChoice("CORNER", Field.ParkChoice.CORNER_PARK, allianceMenu);

        allianceMenu.addChoice("RED",  Field.Alliance.RED,  teamMenu);
        allianceMenu.addChoice("BLUE", Field.Alliance.BLUE, teamMenu);

        teamMenu.addChoice("Sonic",   Team.SONIC);
        teamMenu.addChoice("Snowman", Team.SNOWMAN);

        testDistMenu.setChildMenu(testSpdMenu);
        testSpdMenu.setChildMenu(testKMenu);

        //
        // Walk the menu tree starting with the strategy menu as the root
        // menu and get user choices.
        //
        FtcMenu.walkMenuTree(startPosMenu);
        //
        // Set choices variables.
        //
        //autoStrategy = (Field.AutoStrategy)strategyMenu.getCurrentChoiceObject();

        startPos = (Field.StartPos)startPosMenu.getCurrentChoiceObject();
        beaconChoice = (Field.BeaconChoice)pushMenu.getCurrentChoiceObject();
        parkChoice = (Field.ParkChoice)parkMenu.getCurrentChoiceObject();
        alliance = (Field.Alliance)allianceMenu.getCurrentChoiceObject();
        team = (Team)teamMenu.getCurrentChoiceObject();
        testDist  = testDistMenu.getCurrentValue();
        testSpeed = testSpdMenu.getCurrentValue();
        testK     = testKMenu.getCurrentValue();

        //dashboard.displayPrintf(0, "STRATEGY: %s", autoStrategy);
        dashboard.displayPrintf(0, "START: %s", startPos);
        dashboard.displayPrintf(1, "PUSH: %s", beaconChoice);
        dashboard.displayPrintf(2, "PARK: %s", parkChoice);
        dashboard.displayPrintf(3, "ALLIANCE: %s", alliance);
        dashboard.displayPrintf(4, "TEAM: %s", team);
        dashboard.displayPrintf(5, "TestK:", "4.2f", testK);
    }

    private enum ButtonSide
    {
        UNKNOWN,
        LEFT,
        RIGHT
    }

    private enum Team
    {
        SONIC,
        SNOWMAN
    }

    private final static double ZER_PUSH_POS = 0.0;
    private final static double RGT_PUSH_POS = 0.2;
    private final static double LFT_PUSH_POS = 0.8;
    private final static double CTR_PUSH_POS = 0.5;

    //private final static double DEF_DRV_PWR  = 0.7;
    private final static double DEF_TRN_PWR  = 0.45;

    private final static double SHT_PWR_SONIC = 0.50;
    private final static double SHT_PWR_SNOWMAN = 0.50;
    private static double DEF_SHT_PWR = SHT_PWR_SONIC;
    private final static double DEF_SWP_PWR = 1.0;
    private final static double DEF_ELV_PWR = 0.5;

    private Segment[] pathSegs;

    private ShelbyBot   robot = new ShelbyBot();
    private ElapsedTime timer = new ElapsedTime();
    private Drivetrain drvTrn = new Drivetrain();

    private ImageTracker tracker;
    private BeaconDetector bd = new BeaconDetector();
    private ButtonSide bSide = ButtonSide.UNKNOWN;

    private static Point2d curPos;
    private static double  curHdg;

    private static Field.AutoStrategy autoStrategy =
            Field.AutoStrategy.SHOOT_PARKCNTR;

    private static Field.StartPos startPos = Field.StartPos.START_A;
    private static Field.BeaconChoice beaconChoice = Field.BeaconChoice.NEAR;
    private static Field.ParkChoice parkChoice = Field.ParkChoice.CENTER_PARK;
    private static Field.Alliance alliance = Field.Alliance.RED;
    private static Team team = Team.SONIC;

    private HalDashboard dashboard;

    private double testDist  = 3.0;
    private double testSpeed = 0.5;
    private double testK = 1.0;

    private double initHdg = 0.0;
}


