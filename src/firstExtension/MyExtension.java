package firstExtension;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.match.MatchExpression;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.SFSExtension;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import sfs.lms.A;
import sun.util.calendar.CalendarUtils;

import java.util.*;
import java.util.function.Function;

import java.util.stream.Collectors;

public class MyExtension extends SFSExtension {

   public static class MyRoom {
        List<User> userList;

        public void SendMessage(String message) {
            //send userList

        }
    }

    static int result =0;

    static List<Integer> listOfIntegers = new ArrayList<>();
    static List<Integer> roomIdLists = new ArrayList<>();
    static List<Room> room = new ArrayList<>();

    static class Calculator{
        int a =0;
        int b=0;

        Calculator(int x,int y){
             a= x;
             b= y;
        }
        int sum(){
            result += a+b;
            return a+b;
        }

        int mul(){
            result+=a*b;
            return a*b;
        }

    }



    @Override
    public void init() {

        trace("Hello, this is my first SFS2X Extension!");
        addEventHandler(SFSEventType.SERVER_READY, (ISFSEvent event) -> {
            CreateRoomSettings cfg = new CreateRoomSettings();
            cfg.setName("roomTest");
            cfg.setGroupId("text");
            cfg.setMaxUsers(30);
            cfg.setDynamic(true); // In order for the auto-remove feature to be active, the isDynamic flag of the Room must be set to true.
            cfg.setExtension(new CreateRoomSettings.RoomExtensionSettings("myFirstExtension","firstExtension.RoomExtension"));
            cfg.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);

            try {
                getApi().createRoom(getParentZone(), cfg, null);
            } catch (SFSCreateRoomException e) {
                e.printStackTrace();
            }
        });



        // Add a new Request Handler
        addRequestHandler("list", getListsHandler.class);
        addRequestHandler("sum", SumReqHandler.class);
        addRequestHandler("mul", multiHandler.class);
        addRequestHandler("room", makeRoomHandler.class);
        addRequestHandler("show", showRoomHandler.class);
        addRequestHandler("join", joinRoomHandler.class);
        addRequestHandler("msg", sendMsgRoomHandler.class);

    }


    public static class makeRoomHandler extends BaseClientRequestHandler{
        @Override
        public void handleClientRequest(User sender, ISFSObject params) {
            ISFSObject resObj = new SFSObject();
            int index =getParentExtension().getParentZone().getRoomList().size();
            String roomName = params.getUtfString("roomName")+"_"+String.valueOf(index);
            //Check if Room is exist already

            /*create Room*/
            CreateRoomSettings createRoomSettings = new CreateRoomSettings();
            createRoomSettings.setName(roomName+(index-1));

            /*set RoomId In List, and Send Results*/
            try {
                /* return this.parentExtension.sfsApi */
                getApi().createRoom( getParentExtension().getParentZone(),createRoomSettings,sender);
                trace(getParentExtension().getParentZone().getRoomList());
                roomIdLists.add(getParentExtension().getParentZone().getRoomList().get(index).getId());
                room.add(getParentExtension().getParentZone().getRoomList().get(index));
                resObj.putInt("result",0);
                resObj.putInt("roomId",roomIdLists.get(index));
                send("room",resObj,sender);

            } catch (SFSCreateRoomException e) {
                trace(e);
            }
        }
    }

    public static class showRoomHandler extends BaseClientRequestHandler{
        @Override
        public void handleClientRequest(User sender, ISFSObject params) {
            ISFSObject resObj = new SFSObject();
            ISFSArray rooms = new SFSArray();

            int roomsSize = roomIdLists.size();

            for(int i=0 ; i<roomsSize ; i++){
                rooms.addInt(roomIdLists.get(i));
            }

            resObj.putSFSArray("list",rooms);
            send("showRoom",resObj,sender);


        }
    }

    public static class joinRoomHandler extends BaseClientRequestHandler{
        @Override
        public void handleClientRequest(User sender, ISFSObject params) {
            ISFSObject resObj = new SFSObject();

           // Room newRoom = getApi().createRoom();



            int roomId =params.getInt("id");

            /*Find Room with Id & enter*/

            List<Room> joiningRoom =room.stream().filter((s)-> s.getId()==roomId).collect(Collectors.toList());

            try {
                getApi().joinRoom(sender,joiningRoom.get(0));
                trace("Joining Room = "+joiningRoom.get(0).getName());
            } catch (SFSJoinRoomException e) {
                e.printStackTrace();
            }

            List<User> usersInRoom = joiningRoom.get(0).getUserList();
            for(User user :usersInRoom ){
                if(!(user.getId() ==sender.getId())){
                    getApi().sendAdminMessage(user,"SomeoneJoined",null,null);
                    send("newUserEnter",resObj,user);
                }
            }



        }
    }

    public static class sendMsgRoomHandler extends BaseClientRequestHandler{
        @Override
        public void handleClientRequest(User sender, ISFSObject params) {
            ISFSObject resObj = new SFSObject();

            // sender.getId()

            /*get room */
            int roomId =params.getInt("roomId");
            //trace("roomCheck => "+getParentExtension().getParentRoom().getName());

            /*Find Room with Id & enter*/
            List<Room> joiningRoom =room.stream().filter((s)-> s.getId()==roomId).collect(Collectors.toList());
            List<User> usersInRoom = joiningRoom.get(0).getUserList();
            resObj.putUtfString("msg", params.getUtfString("msg"));
            send("msg",resObj,usersInRoom);
            trace("Joining Room = "+joiningRoom.get(0).getName());



        }
    }

    /*Sum Handler*/
    public static class SumReqHandler extends BaseClientRequestHandler {
        @Override
        public void handleClientRequest(User sender, ISFSObject params) {

            /* Get the client parameters for Constructor*/
            int n1 = params.getInt("n1");
            int n2 = params.getInt("n2");

            /*Setting Calculator class variables with Constructor*/
            Calculator cal = new Calculator(n1, n2);

            /*Call Method*/
            int r = cal.sum();

            // Create a response object
            ISFSObject resObj = new SFSObject();
            resObj.putInt("res",result);

            /*add List to check histories*/
            listOfIntegers.add(result);

            /*Get All users from parents Extension*/
            List<User> existUserList = getParentExtension().getParentZone().getUserManager().getAllUsers();

            /*Send Results And notice depend on users */
            for(User users : existUserList){

                if(users.getId()==sender.getId()){ //To sender
                    trace("sending To Sender");
                    send("sum", resObj, sender);
                }else{
                    trace("Sending TO Everyone");
                    send("notice", resObj, users); // To all Users
                }
            }
        }
    }

    /*List Handler*/
    public static class getListsHandler extends BaseClientRequestHandler {
        @Override
        public void handleClientRequest(User sender, ISFSObject params) {
            // Create a response object
            ISFSObject resObj = new SFSObject();
            ISFSArray arrayofResults = new SFSArray();

            int arrayListSize = listOfIntegers.size();
            for(int i = 0; i<arrayListSize; i++){
                arrayofResults.addInt(listOfIntegers.get(i));
            }

            //arrayofResults.addInt(listOfIntegers);

            trace("sending list : "+listOfIntegers);
            resObj.putSFSArray("res",arrayofResults);
            // Send it back
            trace("list");
             send("list", resObj, sender);
        }
    }

    /*Multi handler*/
    public static class multiHandler extends BaseClientRequestHandler {
        @Override
        public void handleClientRequest(User sender, ISFSObject params) {
            Calculator cal = new Calculator(params.getInt("n1"),params.getInt("n2"));
            cal.mul();

            //Create new array for resObj
            ISFSArray array = new SFSArray();
            array.addInt(result);
           // resObj.putSFSArray("resultOf_"+sender.getName(), array);
            // Create a response object
            //ISFSObject resObj = new SFSObject();

            // Send it back to client sender
            //send("multi", resObj, getParentExtension().getParentZone().getUserManager().getAllUsers());
            //send notification to every client
        }
    }
}