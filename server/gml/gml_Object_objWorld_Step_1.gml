var query, heldString, pressedString, releasedString, i, button, flags;
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
            heldString = network_check_text(1)
            if is_undefined(heldString)
            {
                scrCheckTASIntegrity()
                exit
            }
            pressedString = network_check_text(1)
            if is_undefined(pressedString)
            {
                scrCheckTASIntegrity()
                exit
            }
            releasedString = network_check_text(1)
            if is_undefined(releasedString)
            {
                scrCheckTASIntegrity()
                exit
            }
            scrAddTASOverrides(heldString, 1)
            scrAddTASOverrides(pressedString, 4)
            scrAddTASOverrides(releasedString, 16)
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
        case "room_goto":
            room_goto(asset_get_index(network_check_text(1)))
            break
        case "savestate":
            with (objPlayer)
            {
                flags = (((((((((djump | (frozen << (1 << 0))) | (onPlatform << (2 << 0))) | (global.runSwitch << (3 << 0))) | (global.complexMode << (4 << 0))) | (aura[0] << (5 << 0))) | (aura[1] << (6 << 0))) | (aura[2] << (7 << 0))) | (recentJump << (8 << 0))) | (global.fAnimIsNormal << (9 << 0)))
                network_send_text(global.tasPrevHeld, " ", x, " ", y, " ", vspeed, " ", flags, " ", masterMode, " ", brownMode, " ", downTime, " ", downDir, " ", global.fAnimTimer, " ", global.fAnimSpd)
            }
            if (!instance_exists(objPlayer))
            {
                flags = (((global.runSwitch << (3 << 0)) | (global.complexMode << (4 << 0))) | (global.fAnimIsNormal << (9 << 0)))
                network_send_text(global.tasPrevHeld, " 0 0 0 ", flags, " 0 0 0 0 ", global.fAnimTimer, " ", global.fAnimSpd)
            }
            for (i = 0; i < 16; i++)
                network_send_text(global.key[i], " ", global.ikey[i])
            with (oKeyBulk)
                network_send_text((active | (touched << (1 << 0))))
            with (oDoorSimple)
            {
                flags = (((((active | (aura[0] << (1 << 0))) | (aura[1] << (2 << 0))) | (aura[2] << (3 << 0))) | (browned << (4 << 0))) | (brownNearPlayer << (5 << 0)))
                network_send_text(flags, " ", copies, " ", icopies, " ", copyState, " ", copyTimer)
            }
            with (oDoorCombo)
            {
                flags = (((((active | (aura[0] << (1 << 0))) | (aura[1] << (2 << 0))) | (aura[2] << (3 << 0))) | (browned << (4 << 0))) | (brownNearPlayer << (5 << 0)))
                network_send_text(flags, " ", copies, " ", icopies, " ", copyState, " ", copyTimer)
            }
            network_send_text("end")
            break
        case "load_savestate":
            global.inCutscene = 0
            with (oLevelWin)
                instance_destroy()
            with (oLevelWinS)
                instance_destroy()
            with (oDebrisS)
                instance_destroy()
            with (oKeyPart)
                instance_destroy()
            query = network_check_text(1)
            if (!instance_exists(objPlayer))
                instance_create(0, 0, objPlayer)
            with (objPlayer)
            {
                string_split_initialize(query)
                global.tasPrevHeld = string_split_next_int()
                x = string_split_next_float()
                y = string_split_next_float()
                xprevious = x
                yprevious = y
                vspeed = string_split_next_float()
                flags = string_split_next_int()
                djump = ((flags & 1) != false)
                frozen = ((flags & 2) != false)
                onPlatform = ((flags & 4) != false)
                global.runSwitch = ((flags & 8) != false)
                global.complexMode = ((flags & 16) != false)
                aura[0] = ((flags & 32) != false)
                aura[1] = ((flags & 64) != false)
                aura[2] = ((flags & 128) != false)
                recentJump = ((flags & 256) != false)
                global.fAnimIsNormal = ((flags & 512) != false)
                masterMode = string_split_next_int()
                brownMode = string_split_next_int()
                downTime = string_split_next_int()
                downDir = string_split_next_int()
                global.fAnimTimer = string_split_next_int()
                global.fAnimSpd = string_split_next_float()
            }
            for (i = 0; i < 16; i++)
            {
                string_split_initialize(network_check_text(1))
                global.key[i] = string_split_next_float()
                global.ikey[i] = string_split_next_float()
            }
            with (oKeyBulk)
            {
                flags = real(network_check_text(1))
                active = ((flags & 1) != false)
                touched = ((flags & 2) != false)
                visible = active
                undoReposition()
            }
            with (oDoorSimple)
            {
                string_split_initialize(network_check_text(1))
                flags = string_split_next_int()
                active = ((flags & 1) != false)
                aura[0] = ((flags & 2) != false)
                aura[1] = ((flags & 4) != false)
                aura[2] = ((flags & 8) != false)
                browned = ((flags & 16) != false)
                brownNearPlayer = ((flags & 32) != false)
                copies = string_split_next_float()
                icopies = string_split_next_float()
                copyState = string_split_next_int()
                copyTimer = string_split_next_int()
                copySound = -1
                visible = active
                undoReposition()
            }
            with (oDoorCombo)
            {
                string_split_initialize(network_check_text(1))
                flags = string_split_next_int()
                active = ((flags & 1) != false)
                aura[0] = ((flags & 2) != 0)
                aura[1] = ((flags & 4) != 0)
                aura[2] = ((flags & 8) != 0)
                browned = ((flags & 16) != 0)
                brownNearPlayer = ((flags & 32) != 0)
                copies = string_split_next_float()
                icopies = string_split_next_float()
                copyState = string_split_next_int()
                copyTimer = string_split_next_int()
                copySound = -1
                visible = active
                undoReposition()
            }
            break
        case "obstacles":
            with (objBlock)
                network_send_text(object_index, " ", x, " ", y, " ", image_xscale, " ", image_yscale, " ", bbox_left, " ", bbox_top, " ", ((bbox_right - bbox_left) + 1), " ", ((bbox_bottom - bbox_top) + 1))
            network_send_text("end")
            break
        case "other_stuff":
            with (oKeyBulk)
                network_send_text(object_index, " ", x, " ", y, " ", sprite_index)
            with (oOmegaKey)
                network_send_text(object_index, " ", x, " ", y, " ", sprite_index)
            with (oSalvageIn)
                network_send_text(object_index, " ", x, " ", y, " ", sprite_index)
            with (oSalvageOut)
                network_send_text(object_index, " ", x, " ", y, " ", sprite_index)
            with (oGoal)
                network_send_text(object_index, " ", x, " ", y, " ", sprite_index)
            with (oLevel)
                network_send_text(object_index, " ", x, " ", y, " ", sprite_index)
            network_send_text("end")
            break
        case "end_frame":
            scrCheckTASIntegrity()
            exit
    }

}
scrCheckTASIntegrity()
