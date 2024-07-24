var _isPart2;
global.runSwitch = 0
scrInitializeGlobals()
network_create_text_server("127.0.0.1", 13785)
if file_exists("config.ini")
{
    ini_open("config.ini")
    _isPart2 = ini_read_real("Part2Check", "Check", 0)
    ini_close()
    if (!_isPart2)
        file_delete("config.ini")
}
scrLoadConfigNew()
room_goto(rTitleNew)
