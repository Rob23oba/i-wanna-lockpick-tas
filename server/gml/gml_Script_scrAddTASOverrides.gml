var i, ch, pos, n;
n = string_length(argument0)
for (i = 1; i <= n; i++)
{
    ch = string_char_at(argument0, i)
    pos = string_pos(ch, global.tasString)
    if ((pos > 0))
        global.tasOverrides[(pos - 1)] |= (argument1 * 3)
    else
    {
        pos = string_pos(ch, global.tasStringOff)
        if ((pos > 0))
        {
            global.tasOverrides[(pos - 1)] |= argument1
            global.tasOverrides[(pos - 1)] &= (~((argument1 * 2)))
        }
    }
}
