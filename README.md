# BedrockMiner
A Fabric mod for Simple Bedrock Breaking. Support 1.21.3

Thanks https://github.com/LXYan2333/Fabric-Bedrock-Miner for the concept of bedrock breaking.

Still glichy, not sure i will fix it all or continue the mod.

## Requirement
- Efficiency V diamond (or netherite) pickaxe
- Haste II
- 2 or above Pistons
- 1 or above Redstone torches

Optional: Slime blocks

## How to use
- Click block by bare hand, diamond or netherite pickaxe

## Command
- /bedrockminer (main switch)
- /bedrockminer list add/del \<block\> (edit breakable blocks)
- /bedrockminer clearTask (clear all tasks)
- /bedrockminer cleanupDelay \<int\> (set delay before cleanup)


## Note
Currently not supported
1. Sideway bedrock breaking
2. Avoid logging of `Mismatch in destroy block pos:` from ServerPlayerGameMode (Paper server switch it to Debug-logging only)
3. Hand Reach check / World check
4. Bypass Hack Plugin/Mod (place from air, looking packets, etc)
5. Torch above piston checking
6. Area/Auto breaking?
