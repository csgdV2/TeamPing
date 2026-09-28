# TeamPing

Inspired by **Lunar Client's Marker** feature.

TeamPing is a **client-side** Fabric mod for Minecraft **1.21.11**. Press a
rebindable key to ping whatever you're looking at — a block, a player, or any
entity — and send that location straight to your friends. Everyone running the
mod sees a clean floating in-world marker and a color-coded chat line.

No server plugin required. It works on any server where private messages are
enabled.

## Features

- **One-key ping** (default `R`, rebindable in Controls). Marks the block you're
  aiming at, a player or entity if one is in your crosshair, or a point ahead of
  you if you aren't aiming at anything. No distance limit.
- **Player & entity pings**: pinging a player shows their name — or
  `Unknown Player` if they're invisible to you, so it stays fair.
- **Per-server friend lists**, saved to disk. Each server keeps its own list.
- **Name autocomplete**: start typing in the friends screen and matching online
  players are suggested; click to add, click a friend to remove. You never show
  up in your own list.
- **In-world marker**: a diamond above a `Name's Marker` label. The diamond flips
  above or below the label depending on where it is on screen, and the marker
  holds a constant on-screen size at any distance.
- **Hover details**: show owner, description (block/entity icon or name),
  distance, and coordinates — each independently set to Always, On Hover, or
  Never, with a smooth hover fade.
- **Smart fade**: markers fade out as you get within ~2 blocks and fade back in
  as you walk away, so they never sit in your face..
- **Same-dimension only**: you only receive a ping, message, and marker when
  you're in the same dimension as the person who placed it.
- **Configurable ping sound**: play it for everyone's pings, only your own, or
  none.
- **Auto-cleanup**: markers expire after a configurable time (default 20s).
- **One ping per player**: a new ping from someone replaces their previous one.
- **Clear-all keybind** (unbound by default) removes every active marker at once.

## Setup

- **Keybinds:** Options → Controls → TeamPing — "Send Location Ping" (default `R`)
  and "Clear All Markers" (unbound by default).
- **Friends:** Mod Menu → TeamPing opens the friends screen. Type a name (online
  players are suggested) and click to add; click a friend to remove. Lists are
  saved per server. The "Settings" button opens all marker, hover, sound, and
  color options.
- **Commands:** `/teamping add <name>`, `/teamping remove <name>`, `/teamping list`.

## Dependencies

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config) — required (settings screen)
- [Mod Menu](https://modrinth.com/mod/modmenu) — optional (opens settings in-game)