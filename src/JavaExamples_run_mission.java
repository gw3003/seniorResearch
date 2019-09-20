// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

// To compile:  javac -cp MalmoJavaJar.jar JavaExamples_run_mission.java
// To run:      java -cp MalmoJavaJar.jar:. JavaExamples_run_mission  (on Linux)
//              java -cp MalmoJavaJar.jar;. JavaExamples_run_mission  (on Windows)

// To run from the jar file without compiling:   java -cp MalmoJavaJar.jar:JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission (on Linux)
//                                               java -cp MalmoJavaJar.jar;JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission (on Windows)

import com.microsoft.msr.malmo.*;

public class JavaExamples_run_mission
{
    static
    {
        System.loadLibrary("MalmoJava"); // attempts to load MalmoJava.dll (on Windows) or libMalmoJava.so (on Linux)
    }

    public static void main(String argv[]) throws Exception
    {
        AgentHost agent_host = new AgentHost();
        try
        {
            StringVector args = new StringVector();
            args.add("JavaExamples_run_mission");
            for( String arg : argv )
                args.add( arg );
            agent_host.parse( args );
        }
        catch( Exception e )
        {
            System.err.println( "ERROR: " + e.getMessage() );
            System.err.println( agent_host.getUsage() );
            System.exit(1);
        }
        if( agent_host.receivedArgument("help") )
        {
            System.out.println( agent_host.getUsage() );
            System.exit(0);
        }
        //Mission XML String
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>"
        +"\n <Mission xmlns=\"http://ProjectMalmo.microsoft.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
        +"\n<About>"
        +  "\n <Summary>Hello world!</Summary>"
        +"\n</About>"
        +"\n<ServerSection>"
        +  "<ServerInitialConditons>"
        +      "<Time>"
        +        "<StartTime>12000</StartTime>"
        +        "<AllowPassageOfTime>false</AllowPassageOfTime>"
        +      "</Time>"
        +  "</ServerInitialConditions>"
        +  "\n<ServerHandlers>"
        +    "\n<FlatWorldGenerator generatorString=\"3;7,220*1,5*3,2;3;,biome_1\"/>"
        +    "\n<ServerQuitFromTimeUp timeLimitMs=\"10000\"/>"
        +    "\n<ServerQuitWhenAnyAgentFinishes/>"
        +  "\n</ServerHandlers>"
        +"\n</ServerSection>"
        +"\n<AgentSection mode=\"Survival\">"
        +"\n<Name>MalmoTutorialBot</Name>"
        +"\n<AgentStart/>"
        +  "\n<AgentHandlers>"
        +    "\n<ObservationFromFullStats/>"
        +    "\n<ContinuousMovementCommands turnSpeedDegs=\"30\"/>"
        +    "\n</AgentHandlers>"
        +  "\n</AgentSection>"
        +"\n</Mission>";


        //Handles creating world
        MissionSpec my_mission = new MissionSpec(xml, true);
        //my_mission.timeLimitInSeconds(10); use xml file
        my_mission.requestVideo( 520, 520 );
        //my_mission.rewardForReachingPosition(19.5f,0.0f,19.5f,100.0f,1.1f);
        
        System.out.println(my_mission.getAsXML(true));
        

        MissionRecordSpec my_mission_record = new MissionRecordSpec("./saved_data.tgz");
        my_mission_record.recordCommands();
        my_mission_record.recordMP4(20, 400000);
        my_mission_record.recordRewards();
        my_mission_record.recordObservations();
        
        ClientPool my_client_pool = new ClientPool();
        my_client_pool.add(new ClientInfo("127.0.0.1", 10000));

        try {
            agent_host.startMission( my_mission, my_client_pool ,my_mission_record, 0, "0");
        }
        catch (MissionException e) {
            System.err.println( "Error starting mission: " + e.getMessage() );
            System.err.println( "Error code: " + e.getMissionErrorCode() );
            // We can use the code to do specific error handling, eg:
            if (e.getMissionErrorCode() == MissionException.MissionErrorCode.MISSION_INSUFFICIENT_CLIENTS_AVAILABLE)
            {
                // Caused by lack of available Minecraft clients.
                System.err.println( "Is there a Minecraft client running?");
            }
            System.exit(1);
        }

        WorldState world_state;

        System.out.print( "Waiting for the mission to start" );
        do {
            System.out.print( "." );
            try {
                Thread.sleep(100);
            } catch(InterruptedException ex) {
                System.err.println( "User interrupted while waiting for mission to start." );
                return;
            }
            world_state = agent_host.getWorldState();
            for( int i = 0; i < world_state.getErrors().size(); i++ )
                System.err.println( "Error: " + world_state.getErrors().get(i).getText() );
        } while( !world_state.getIsMissionRunning() );
        System.out.println( "" );

        //System.out.println(my_mission.getAsXML(true));
        
        // main loop:
        do {
        	
            agent_host.sendCommand( "move 1" );
            agent_host.sendCommand( "turn " + Math.random() );
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex) {
                System.err.println( "User interrupted while mission was running." );
                return;
            }
            world_state = agent_host.getWorldState();
            System.out.print( "video,observations,rewards received: " );
            System.out.print( world_state.getNumberOfVideoFramesSinceLastState() + "," );
            System.out.print( world_state.getNumberOfObservationsSinceLastState() + "," );
            System.out.println( world_state.getNumberOfRewardsSinceLastState() );
            for( int i = 0; i < world_state.getRewards().size(); i++ ) {
                TimestampedReward reward = world_state.getRewards().get(i);
                System.out.println( "Summed reward: " + reward.getValue() );
            }
            for( int i = 0; i < world_state.getErrors().size(); i++ ) {
                TimestampedString error = world_state.getErrors().get(i);
                System.err.println( "Error: " + error.getText() );
            }
        } while( world_state.getIsMissionRunning() );

        System.out.println( "Mission has stopped." );
    }
}
