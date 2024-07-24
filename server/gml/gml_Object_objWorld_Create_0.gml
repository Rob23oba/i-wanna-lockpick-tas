if ((instance_number(object_index) > 1))
    instance_destroy()
global.copyUniAmt = 0
global.copyUniTime = shader_get_uniform(shdRainbowStripe2, "time")
global.doorHue = 0
global.doorCol1 = make_color_hsv(global.doorHue, 125, 255)
global.doorCol2 = make_color_hsv(global.doorHue, 180, 230)
global.doorCol3 = make_color_hsv(global.doorHue, 255, 190)
global.runSwitch = 0
global.salvageA = 0
global.salvageCol = make_color_hsv(140, 150, 255)
runMsgA = 360
runMsgAlpha = 0
pauseAlpha = 0
pauseFade = 0
if (!file_exists("gm_text_server.dll"))
{
    show_message("Cannot locate gm_text_server.dll")
    game_end()
}
external_call(external_define("gm_text_server.dll", "init_extension", 0, 0, 0))
blockingMode = 0
