var query, heldString, pressedString, releasedString, i, button;
global.tasOverrides = 0
global.tasOverrides[16] = 0
global.tasPrevHeld = global.tasCurHeld
scrCheckTASIntegrity()
if (!network_has_client())
{
    blockingMode = 0
    room_speed = 50
    exit
}
if blockingMode
    network_send_text("frame")
while 1
{
    query = network_check_text(blockingMode)
    if is_undefined(query)
    {
        blockingMode = 0
        room_speed = 50
        scrCheckTASIntegrity()
        exit
    }
    switch query
    {
        case "location":
            if instance_exists(objPlayer)
                network_send_text("room=", room, " x=", objPlayer.x, " y=", objPlayer.y, " hspeed=", objPlayer.hspeed, " vspeed=", objPlayer.vspeed)
            else
                network_send_text("room=", room)
            break
        case "set_inputs":
            scrAddTASOverrides(network_check_text(1), 1)
            scrAddTASOverrides(network_check_text(1), 4)
            scrAddTASOverrides(network_check_text(1), 16)
            scrCheckTASIntegrity()
            break
        case "get_inputs":
            heldString = ""
            pressedString = ""
            releasedString = ""
            for (i = 0; i < 17; i++)
            {
                button = scrTASIndexToButton(i)
                if ((button[2] != i))
                    show_message("what")
                if scrButtonCheck(button)
                    heldString += string_char_at(global.tasString, (i + 1))
                else
                    heldString += string_char_at(global.tasStringOff, (i + 1))
                if scrButtonCheckPressed(button)
                    pressedString += string_char_at(global.tasString, (i + 1))
                else
                    pressedString += string_char_at(global.tasStringOff, (i + 1))
                if scrButtonCheckReleased(button)
                    releasedString += string_char_at(global.tasString, (i + 1))
                else
                    releasedString += string_char_at(global.tasStringOff, (i + 1))
            }
            network_send_text(heldString)
            network_send_text(pressedString)
            network_send_text(releasedString)
            break
        case "curprev":
            network_send_text(global.tasPrevHeld)
            network_send_text(global.tasCurHeld)
            break
        case "block":
            blockingMode = 1
            room_speed = 10000
            break
        case "unblock":
            blockingMode = 0
            room_speed = 50
            break
        case "obstacles":
            with (objBlock)
                network_send_text(object_index, " ", x, " ", y, " ", image_xscale, " ", image_yscale, " ", bbox_left, " ", bbox_top, " ", ((bbox_right - bbox_left) + 1), " ", ((bbox_bottom - bbox_top) + 1))
            network_send_text("end")
            break
        case "end_frame":
            scrCheckTASIntegrity()
            exit
    }

}
scrCheckTASIntegrity()
