var buttonIn, override;
buttonIn = argument0
override = global.tasOverrides[buttonIn[2]]
if (override & 1)
    return (override & 2);
if (!global.controllerMode)
    return keyboard_check(buttonIn[0]);
else
    return gamepad_button_check(global.controllerIndex, buttonIn[1]);
