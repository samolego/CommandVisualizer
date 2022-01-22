# Command Visualiser
A fabric mod that adds gui for any ingame command!

https://user-images.githubusercontent.com/34912839/150638182-003845ef-fe95-4803-8ebf-c6aa04104259.mp4

## How to
Left click to go in submenu (if any, otherswise execute), right click to force-execute.
Edit the config file, generated as `minecraftFolder/config/command_visualiser.json`:
* add commands to `commands` field, e.g. `setblock`, `kick`, `summon`
* edit `node2item` map: if you want command subnode named `kaboom` to show as `gunpowder`, add `"": "gunpowder"` in there.
* fill `prefersExecution` with commands you'd like to swap left and right click behaviour for, e. g. `weather set` (silly example) (this will swap L-R click behaviour for `set` node in `weather` command)
