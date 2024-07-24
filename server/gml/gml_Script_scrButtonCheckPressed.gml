var buttonIn, override;
buttonIn = argument0
override = global.tasOverrides[buttonIn[2]]
if (override & 4)
    return (override & 8);
if (!global.controllerMode)
    return keyboard_check_pressed(buttonIn[0]);
else
    return gamepad_button_check_pressed(global.controllerIndex, buttonIn[1]);
