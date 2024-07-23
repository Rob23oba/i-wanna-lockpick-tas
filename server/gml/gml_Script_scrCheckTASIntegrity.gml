var i, button, pressed, released, prevHeld;
global.tasCurHeld = 0
for (i = 0; i < 17; i++)
{
    button = scrTASIndexToButton(i)
    pressed = scrButtonCheckPressed(button)
    released = scrButtonCheckReleased(button)
    prevHeld = (global.tasPrevHeld & (1 << i))
    if (!pressed)
    {
        global.tasOverrides[i] |= 1
        if released
        {
            global.tasOverrides[i] &= -3
            if (!prevHeld)
                global.tasOverrides[i] |= 60
        }
        else if (!prevHeld)
            global.tasOverrides[i] &= -3
        else
            global.tasOverrides[i] |= 2
    }
    else if (!released)
    {
        global.tasOverrides[i] |= 3
        if prevHeld
            global.tasOverrides[i] |= 60
    }
    if scrButtonCheck(button)
        global.tasCurHeld |= (1 << i)
}
