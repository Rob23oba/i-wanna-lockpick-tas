# I Wanna Lockpick TAS

This project aims to create a TAS for the game [I Wanna Lockpick](https://lawatson.itch.io/i-wanna-lockpick).
The most important files are located under `tas/`: they contain the actual inputs for different parts of the TAS and are written in a special language to denote different inputs.

## The TAS language
Uppercase letters indicate pressing a key, lowercase letters indicate releasing a key. The table below shows the different mapping of letters to keys:

Letter | Key name
------ | ---------------------------
L / l  | **L**eft
R / r  | **R**ight
U / u  | **U**p / Interact
D / d  | **D**own
J / j  | **J**ump / Select
Z / z  | Undo
N / n  | Restart / "**N**ew Attempt"
S / s  | **S**kip
P / p  | **P**ause
C / c  | **C**amera
X / x  | Action / Master Key
B / b  | Exit Level / **B**ack
W / w  | **W**arp
A / a  | PDA
E / e  | Run Switch
F / f  | Run / **F**ast
O / o  | Walk / Sl**O**w

To indicate wait times, simply write them as a number, e.g. write `30` to wait 30 frames = 0.6 seconds.

Comments can be initiated using `#` and go until the end of the line.

But most importantly: use `$` followed by the name of a TAS to "insert" it at the current position.

### Examples:
```
# moves right for 10 frames
R 10 r

Jj 1 # small tap

lrudjznspcxbwaefo # releases all buttons

# runs everything in the file "cool_other_file.txt"
$cool_other_file
```

## The server
In order to run a TAS or to control an IWL instance in other ways, you can use the IWL TAS server located in `server/`.
Additionally to adding the GameMaker code in `server/gml/`, you'll also need to compile the GameMaker extension contained within `server/gm_text_server/` into `gm_text_server.dll` which adds several networking commands to GameMaker.

The server is a text-based TCP server running on port 13785 with the following commands:

`end_frame`: Makes the server not process any more commands in the current frame.

`block`: Puts the server into blocking mode: The server sends a `frame` message at the beginning of a frame and only continues processing the frame after `end_frame` is encountered.

`unblock`: Puts the server out of blocking mode: The server now runs at 50 FPS again and will run commands as soon as possible.

`location`: Print out the current room id as well as the player's position and velocity, if possible.

`obstacles`: Prints out a list of all obstacles (objects derived from objBlock) in the format
`<object_index> <x> <y> <xscale> <yscale> <bbx> <bby> <bbwidth> <bbheight>` where bb refers to "bounding box".

`get_inputs`: Prints out three lines containing the buttons held down, pressed and released respectively.
The buttons are encoded according to the table above where uppercase letters mean "held down / pressed / released" and lowercase letters mean "not held down / not pressed / not released".

`set_inputs`: Overrides the current inputs with the next three lines. The lines are encoded similarly to `get_inputs` with the exception that omitted letters are not overriden.

`curprev`: Prints out two lines containing the buttons held down previously (in the previous frame) and currently respectively.
This time, the buttons are encoded using a number which acts as a bitmask: The order of the bits is exactly the order of buttons presented in the table above where "Left" is represented using the bit with value 2^0 and "Walk" is represented using the bit with value 2^16.

## Clients
The clients are located in `clients/src/` and are all currently written in Java. These clients exist:

`iwltas.cli.Playback`: Plays back the inputs provided to stdin. To run TASes from files, you can use `$` (see above).

`iwltas.cli.Recorder`: Records inputs from the game and prints out the commands to stdout.

`iwltas.gui.Visualizer`: Opens a window that allows you to remotely control the game with keyboard input.
