var i;
global.inload = 0
global.newSavefile = 1
global.sawIntro = 0
global.didEscape = 0
global.clearRank = 0
for (i = 0; i < 50; i += 1)
    global.areaReached[i] = 0
global.curArea = 0
for (i = 0; i < 20; i += 1)
    global.seenKeyColor[i] = 0
for (i = 250; i < 350; i += 1)
    global.roomClear[i] = 0
for (i = 0; i < 350; i += 1)
    global.roomSeen[i] = 0
for (i = 0; i < 16; i += 1)
{
    global.omegaKey[i] = 0
    global.omegaPuzzle[i] = 0
}
for (i = 0; i < 5; i += 1)
    global.omegaPerk[i] = 0
for (i = 0; i < 15; i += 1)
    global.areaSuperClear[i] = 0
for (i = 0; i < 20; i += 1)
    global.didCutscene[i] = 0
global.kinaHeart = 0
for (i = 0; i < 100; i += 1)
{
    global.convoUnlocked[i] = 0
    global.convoRead[i] = 0
}
for (i = 0; i < 30; i += 1)
    global.randomVar[i] = 0
for (i = 0; i < 256; i += 1)
{
    global.pmVis[i] = 0
    global.pmX[i] = 0
    global.pmY[i] = 0
    global.pmType[i] = 0
    global.pmSymbol[i] = 0
    global.pmNum[i] = 0
    global.pmStr[i] = 0
    global.pmCol[i] = 0
}
global.canMark = 0
global.curPM = -1
global.pmState = 0
global.musicTitle = ""
global.fAnimIsNormal = 1
global.fAnimTimer = 100
global.fAnimSpd = 0
global.tasPrevHeld = 0
global.tasCurHeld = 0
