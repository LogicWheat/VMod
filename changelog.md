# Next
* Added function "/vmod op prune-shipyard-chunks" to delete chunks of deleted ships
* Added automatic region cleanup on ship deletion
* Fixed "/vmod op clear-vmod-attachments" not working

# 1.7.1
* Fixed bug with "Open or Close Toolgun GUI" keybind stopping working sometimes
* Fixed ~~maybe~~ some issues with schematics
* Fixed bug where ServerLimits reset to default
* Fixed bug where custom per-ship gravity weren't saving

# 1.7.0
* "Open or Close Toolgun GUI" keybind is now ignored while writing in the parameter space
* New config values for schematic control
* Added experimental ship highlighting (doesn't work with sodium)
* Added texture options for rope and phys rope
* Fixed inconsistent texture width and length in ropes
* Fixed HUD not hiding
* Fixed VMod sometimes incorrectly thinking that two ships are connected when they are actually not 
* Internal changes and bugfixes

# 1.6.1
* Fixed precise placement assist side num not updating after changing preset
* Fixed game crashing on pressing delete in Settings Preset menu without choosing preset
* Made GravChangerMode, MassChangerMode, PhysRopeMode, ScaleMode, SensorMode, SliderMode, ThrusterMode, presettable
* Fixed VMod not working on servers 

# 1.6.0
* Added delete button to VEntityChanger
* Added Settings Presets
* Fixed rare crash when raycasting
* Rolled back schematic renderer change cuz it didn't work
* Fixed phys bearing schem compat not working
* Fixed possible crash when changing color in VEntity Changer

# 1.5.1
* Fixed incompat with control craft
* Changed schematic renderer
# 1.5.0
* Added a way to open client/server setting via commands ("/vmod-client open-client-settings", "/vmod-client open-server-settings")
* Added HUD Info Window if you hold toolgun and press "I"
* Fixed issue when placing schematic multiple times
* Fixed create blocks not updating correctly upon schematic placement
* Fixed constrains not correctly deserializing