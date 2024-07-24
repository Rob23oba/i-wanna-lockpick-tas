var i;
ini_open("config.ini")
global.muteMusic = ini_read_real("Sound", "Mute_music", 0)
global.soundLevel = clamp(floor(ini_read_real("Sound", "Sound_level", 80)), 0, 100)
global.musicLevel = clamp(floor(ini_read_real("Sound", "Music_level", 80)), 0, 100)
global.volumeLevel = clamp(floor(ini_read_real("Sound", "Volume_level", 30)), 0, 100)
audio_master_gain((global.volumeLevel / 100))
global.fullscreenMode = ini_read_real("Visual", "Fullscreen_mode", 0)
window_set_fullscreen(global.fullscreenMode)
global.smoothingMode = ini_read_real("Visual", "Smoothing_mode", 0)
global.vsyncMode = ini_read_real("Visual", "Vsync_mode", 0)
if global.vsyncMode
    scrSetVsync()
global.windowScale = ini_read_real("Visual", "windowScale", 1)
global.lowFX = ini_read_real("Visual", "lowFX", 0)
global.simpleLock = ini_read_real("Visual", "simpleLock", 0)
global.pauseStyle = ini_read_real("Visual", "pauseStyle", 0)
for (i = 0; i < 14; i += 1)
{
    global.darkTone[i] = ini_read_real("Colors", ("darkTone" + string(i)), -1)
    global.mainTone[i] = ini_read_real("Colors", ("mainTone" + string(i)), -1)
    global.highTone[i] = ini_read_real("Colors", ("highTone" + string(i)), -1)
}
if ((global.darkTone[0] == -1))
    setColorsDefault()
global.useSymbols = ini_read_real("Colorblind", "useSymbols", 0)
global.doHighlight = ini_read_real("Colorblind", "doHighlight", 0)
global.filter = ini_read_real("Colorblind", "filter", 0)
global.hideTime = ini_read_real("Accessibility", "hideTime", 1)
global.runSwitch = ini_read_real("Accessibility", "runSwitch", 0)
global.fullJumps = ini_read_real("Accessibility", "fullJumps", 0)
global.allUnlock = ini_read_real("Accessibility", "allUnlock", 0)
global.fastAnims = ini_read_real("Accessibility", "fastAnims", 0)
global.leftButton[0] = ini_read_real("Controls", "Left", 37)
global.rightButton[0] = ini_read_real("Controls", "Right", 39)
global.upButton[0] = ini_read_real("Controls", "Up", 38)
global.downButton[0] = ini_read_real("Controls", "Down", 40)
global.jumpButton[0] = ini_read_real("Controls", "Jump", 32)
global.shootButton[0] = ini_read_real("Controls", "Undo", 90)
global.restartButton[0] = ini_read_real("Controls", "Restart", 82)
global.skipButton[0] = ini_read_real("Controls", "Skip", 83)
global.pauseButton[0] = ini_read_real("Controls", "Pause", 27)
global.camButton[0] = ini_read_real("Controls", "Camera", 67)
global.masButton[0] = ini_read_real("Controls", "Action", 88)
global.leaveButton[0] = ini_read_real("Controls", "Leave", 8)
global.warpButton[0] = ini_read_real("Controls", "Warp", 87)
global.specialButton[0] = ini_read_real("Controls", "Special", 65)
global.rsButton[0] = ini_read_real("Controls", "Runswitch", 69)
global.runButton[0] = ini_read_real("Controls", "Run", 16)
global.walkButton[0] = ini_read_real("Controls", "Walk", 17)
global.selButton[0] = ini_read_real("Controls", "Select", 32)
global.intrButton[0] = ini_read_real("Controls", "Interact", 38)
global.menuLeftButton[0] = 37
global.menuRightButton[0] = 39
global.menuUpButton[0] = 38
global.menuDownButton[0] = 40
global.menuAcceptButton[0] = 32
global.menuBackButton[0] = 90
global.menuOptionsButton[0] = 13
if global.controllerEnabled
{
    global.controllerIndex = ini_read_real("Controller", "Index", -1)
    global.leftButton[1] = ini_read_real("Controller", "Left", 32783)
    global.rightButton[1] = ini_read_real("Controller", "Right", 32784)
    global.upButton[1] = ini_read_real("Controller", "Up", 32781)
    global.downButton[1] = ini_read_real("Controller", "Down", 32782)
    global.jumpButton[1] = ini_read_real("Controller", "Jump", 32769)
    global.shootButton[1] = ini_read_real("Controller", "Undo", 32771)
    global.restartButton[1] = ini_read_real("Controller", "Restart", 32772)
    global.skipButton[1] = ini_read_real("Controller", "Skip", 32774)
    global.pauseButton[1] = ini_read_real("Controller", "Pause", 32778)
    global.camButton[1] = ini_read_real("Controller", "Camera", 32775)
    global.masButton[1] = ini_read_real("Controller", "Action", 32770)
    global.leaveButton[1] = ini_read_real("Controller", "Leave", 32776)
    global.warpButton[1] = ini_read_real("Controller", "Warp", 32777)
    global.specialButton[1] = ini_read_real("Controller", "Special", 32773)
    global.rsButton[1] = ini_read_real("Controller", "Runswitch", 32780)
    global.runButton[1] = ini_read_real("Controller", "Run", 32781)
    global.walkButton[1] = ini_read_real("Controller", "Walk", 32782)
    global.selButton[1] = ini_read_real("Controls", "Select", 32769)
    global.intrButton[1] = ini_read_real("Controls", "Interact", 32781)
}
global.leftButton[2] = 0
global.rightButton[2] = 1
global.upButton[2] = 2
global.downButton[2] = 3
global.jumpButton[2] = 4
global.shootButton[2] = 5
global.restartButton[2] = 6
global.skipButton[2] = 7
global.pauseButton[2] = 8
global.camButton[2] = 9
global.masButton[2] = 10
global.leaveButton[2] = 11
global.warpButton[2] = 12
global.specialButton[2] = 13
global.rsButton[2] = 14
global.runButton[2] = 15
global.walkButton[2] = 16
global.selButton[2] = 4
global.intrButton[2] = 2
global.tasString = "LRUDJZNSPCXBWAEFO"
global.tasStringOff = "lrudjznspcxbwafeo"
global.menuLeftButton[1] = 32783
global.menuRightButton[1] = 32784
global.menuUpButton[1] = 32781
global.menuDownButton[1] = 32782
global.menuAcceptButton[1] = 32769
global.menuBackButton[1] = 32770
global.menuOptionsButton[1] = 32777
ini_close()
scrSaveConfigNew()
