package firstExtension;

import com.smartfoxserver.v2.extensions.SFSExtension;

public class RoomExtension extends SFSExtension {

    @Override
    public void init() {
        trace("My Room Extension properly Working!");
        //addRequestHandler("msg", MyExtension.sendMsgRoomHandler.class);
    }


}
