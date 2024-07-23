var buttonIn, override;
buttonIn = argument0
override = global.tasOverrides[buttonIn[2]]
if (override & 16)
    return (override & 32);
if (!global.controllerMode)
    return keyboard_check_released(buttonIn[0]);
else
    return gamepad_button_check_released(global.controllerIndex, buttonIn[1]);
