## 🎀 KawaiiAD Plugin: Advanced Advertisement System
<div align="center">

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Paper](https://img.shields.io/badge/Paper-1.21.4+-green.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Status](https://img.shields.io/badge/status-production--ready-success.svg)

KawaiiAD is a highly optimized, production-ready Minecraft plugin designed for Paper servers that provides a robust, confirmation-based system for server-wide advertisements. Utilizing the modern **Adventure API** and **HikariCP** for performance, it ensures a lag-free, fully customizable, and moderator-friendly experience with persistent data storage.
</div>

### ✨ Key Features

| Feature                            | Description                                                                                                                                           | Status        |
|:-----------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------|
| **Confirmation Workflow (`/ads`)** | Players submit an ad, view a preview, and must confirm via a clickable chat component or by typing `/ads confirm`.                                    | ✅ Implemented |
| **High Performance (HikariCP)**    | Cooldowns are managed using a dedicated SQLite database and the industry-standard **HikariCP connection pool**, eliminating lag from database access. | ✅ Implemented |
| **Persistent Cooldowns**           | Cooldowns are stored in a database, ensuring time limits persist across server restarts.                                                              | ✅ Implemented |
| **Ad Review Queue**                | Configurable setting to require staff approval. Submitted ads are sent to a database queue and staff are alerted via chat (`kawaiid.review`).         | ✅ Implemented |
| **Dynamic UX**                     | Action Bar feedback is provided when a player is on cooldown, clearly displaying the time remaining.                                                  | ✅ Implemented |
| **PAPI Integration**               | Full support for PlaceholderAPI (PAPI) placeholders in submitted advertisements and system messages.                                                  | ✅ Implemented |
| **Moderation Filters**             | Configurable minimum/maximum character limits and a profanity filter.                                                                                 | ✅ Implemented |
| **Cooldown Tiers**                 | Configure different cooldown lengths based on player rank permissions (`kawaiid.cooldown.*`).                                                         | ✅ Implemented |

### 🛠️ Commands

| Command                                | Permission       | Description                                                                           |
|:---------------------------------------|:-----------------|:--------------------------------------------------------------------------------------|
| `/ads <message>`                       | `kawaiid.use`    | Submits a new advertisement and initiates the review/confirmation process.            |
| `/ads confirm`                         | `kawaiid.use`    | **(If review is NOT required)** Confirms the pending ad for immediate broadcast.      |
| `/ads cancel`                          | `kawaiid.use`    | Cancels the pending ad request.                                                       |
| `/ads reload`                          | `kawaiid.admin`  | Reloads the `config.yml` and updates all cached settings instantly.                   |
| `/ads broadcast <type> <target> <msg>` | `kawaiid.admin`  | Sends an instant ad to a specific audience (e.g., world, permission group).           |
| `/ads review [page]`                   | `kawaiid.review` | **(Placeholder)** Allows staff to view and manage ads awaiting approval in the queue. |
| `/kawaiiadshelp`                       | `kawaiid.use`    | Displays the rich, interactive help menu.                                             |

### 🔑 Permissions

| Permission Node        | Default | Description                                                         |
|:-----------------------|:--------|:--------------------------------------------------------------------|
| `kawaiid.use`          | `op`    | Required to submit, confirm, or cancel ads.                         |
| `kawaiid.admin`        | `op`    | Grants access to `/ads reload` and all `/ads broadcast` commands.   |
| `kawaiid.review`       | `op`    | Grants access to the Ad Review Queue commands and staff alerts.     |
| `kawaiid.bypass`       | `op`    | Allows the player to submit ads instantly, bypassing all cooldowns. |
| `kawaiid.cooldown.vip` | `false` | Assigns the shorter "vip" cooldown tier.                            |

### ⚙️ Configuration (`config.yml` Highlights)

The configuration file is generated upon first run and contains detailed comments.

```yaml
settings:
  # If true, detailed information about DB loads, saves, and command flow is logged.
  debug-mode: false
  
cooldowns:
  default: 300 # 5 minutes default cooldown (in seconds)
  ranks:
    vip: 60    # Players with 'kawaiid.cooldown.vip' wait 60 seconds.

moderation:
  # If true, all non-admin submitted ads go into a database queue for manual staff approval.
  require-review: false
  min-length: 10
  max-length: 150
  profanity-filter:
    - "badword"
    - "anotherbadword"

messages:
  # Example of a message using a placeholder for time remaining (used in action bar and chat)
  on-cooldown: "&cYou must wait <time_remaining> before sending another ad."
  ```

### Credits

- Author: [oumaimaa](https://github.com/4K1D3V)
- Team: [KawaiiDevelopmentMC](https://github.com/KawaiiDevelopmentMC)

<div align="center">

**Made with ❤️ by oumaimaa**

⭐ Star this repo if you find it useful!

**Version 1.0.0** - Production Ready

[⬆ Back to top](#-kawaiiad-plugin-advanced-advertisement-system)

</div>