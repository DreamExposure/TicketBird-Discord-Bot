# TicketBird
[![Discord](https://img.shields.io/discord/375357265198317579?label=DreamExposure&style=flat-square)](https://discord.gg/2TFqyuy)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/DreamExposure/TicketBird-Discord-Bot/gradle.yml?label=Build&style=flat-square)
[![Website](https://img.shields.io/website?down_color=red&down_message=offline&label=Status&style=flat-square&up_message=online&url=https%3A%2F%2Fticketbird.dreamexposure.org)](https://ticketbird.dreamexposure.org)

TicketBird is a simple help desk and ticket managing Discord bot allowing you to run a simple help desk system within your Discord server. 
Perfect for modmail, community management, and more, TicketBird is capable of all your ticketing needs!

# 🔗 Quick Links
- [Invite](https://discord.com/oauth2/authorize?client_id=456140067220750336&permissions=395405945880&scope=bot+applications.commands)
- [Website](https://ticketbird.dreamexposure.org) (Will be rewritten eventually)
- [Discord support server](https://discord.gg/2TFqyuy)

# 💎 Core Features
- Simple setup (just use one command to get everything initiated)
- Now with customizable messages!
- Easy to repair. Accidentally delete a channel? No worries, TicketBird is always watching for changes and ready to repair itself via a single command!
- Easy for users to understand and use without a single command (but can also open tickets with a command)
- Allow users to pick from up to 25 topics (called projects), if desired
- Won't disrupt your community. TicketBird will not affect anything beyond the channels it manages for tickets.
- Auto-close tickets after 7 days of inactivity, and auto-delete tickets after being closed for 24 hours (fully configurable)
- Place tickets on hold until you can get to them; so they don't auto-close
- Set staff per project/topic using the `/project edit` command
  - This allows you to define a user or role as staff for only a specific ticket type, allowing further customization similar to traditional RBAC.
- Ping specific staff members and roles when a new ticket is opened
  - Can be overridden on a project-level basis
- Ticket logging
  - Log files can be validated with a SHA-256 file checksum

## ⌨️ Commands
| Command                  | Description                                                                               | Permissions |
|--------------------------|-------------------------------------------------------------------------------------------|-------------|
| /ticketbird              | Shows info about the bot                                                                  | Everyone    |
| /support                 | Opens a new ticket                                                                        | Everyone    |
| /topic                   | Changes a ticket's topic                                                                  | Everyone    |
| /close                   | Closes a ticket when run in a ticket channel                                              | Everyone    |
| /hold                    | Places a ticket on hold when run in a ticket channel                                      | Everyone    |
| /setup init              | Lets the bot setup by creating needed channels/categories                                 | Admin-only  |
| /setup repair            | Attempts to automatically repair any configuration issues                                 | Admin-only  |
| /setup language          | Allows you to select the language for the bot to use                                      | Admin-only  |
| /setup use-projects      | Whether to use "projects" or ticket topics to help sort tickets                           | Admin-only  |
| /setup show-ticket-stats | Whether to show opened/closed ticket stats in the support request embed                   | Admin-only  |
| /setup messaging         | Allows configuring certain messages found throughout TicketBird                           | Admin-only  |
| /setup timing            | Allows configuring the timing of TicketBird's automated actions                           | Admin-only  |
| /setup ping              | Allows modifying the default ping setting when a new ticket is opened                     | Admin-only  |
| /setup logging           | Allows enabling and configuring ticket logging                                            | Admin-only  |
| /setup view              | Allows viewing TicketBird's configuration in the current server                           | Admin-only  |
| /staff role              | Allows users with the role to see all tickets as "TicketBird Staff"                       | Admin-only  |
| /staff add               | Add a user as being "TicketBird Staff" allowing them to see all tickets                   | Admin-only  |
| /staff remove            | Remove a user as "TicketBird Staff"                                                       | Admin-only  |
| /staff list              | List all users who are currently "TicketBird staff"                                       | Admin-only  |
| /project add             | Adds a new "project" or ticket category/topic to aid with sorting                         | Admin-only  |
| /project remove          | Removes an existing project                                                               | Admin-only  |
| /project list            | Lists all existing projects/topics                                                        | Admin-only  |
| /project view            | View full details about a specific project/topic                                          | Admin-only  |
| /project edit            | Edit various settings for a project/topic - Like changing prefix, staff, or ping override | Admin-only  |
| /ticket open             | Opens a new ticket                                                                        | Everyone    | 
| /ticket topic            | Changes a ticket's topic when run in a ticket channel                                     | Everyone    |
| /ticket add              | Adds a user to the ticket as a participant when run in a ticket channel                   | Everyone    |
| /ticket remove           | Removes a user from the ticket when run in a ticket channel                               | Everyone    |
| /ticket hold             | Places a ticket on hold when run in a ticket channel                                      | Everyone    |
| /ticket close            | Closes a ticket when run in a ticket channel                                              | Everyone    |
| /ticket checksum         | Allows validating the authenticity of a TicketBird log file with a SHA-256 checksum       | Everyone    |

# 🗓️ Planned & Work In Progress
This bot is a hobby project for me, please note that while these features are planned, there's no solid timeline.
- Website rewrite (It's old and ugly)
- [WIP] Customizable messages
- And so much more!

# 🧰 Tech stack
- Java 17
- 100% Kotlin utilizing Kotlin Coroutines
- Spring Boot (Data, Dependency Injection, Actuator, etc)
- Flyway for automatic database migrations (MySQL)
- Redis cluster caching
- Enterprise repository & service pattern for maintainability
- Fully containerized with Docker (hosted in Kubernetes, docker-compose for local development)

# ✏️ Contributing
TicketBird is an open source, GPL-3 project. We always welcome and appreciate contributions.

## 💻 Development & Local Testing
For development, you need JDK 17+ and Docker installed.

1. Fork this repository and open it in your favorite editor (IntelliJ recommended for Kotlin)
2. Write your code and add applicable tests
3. Compile and build the docker image with `./gradlew clean jibDockerBuild`
4. Place config in `./docker/bot-config/application.properties`
5. Start the bot and dependencies for testing with `docker compose up -d`
   - You can connect to the Java debugger at port `5005`
6. Create a pull request and describe your changes! <3

# 🌐 Localization (translations)
Please only submit localizations if you speak and/or write the language you are translating to.
We want to keep these translations correct and high quality, running the strings through Google Translate or DeepL is not acceptable.
Thank you for understanding

In the early days of the bot, we had a pretty dis-organized json file system for translated strings.
This was messy and somewhat confusing. Since the 2.0 update, we now utilize properties files

1. The base english locale file is located at `/src/main/resources/locale/values.properties`
2. Files are named `values_{lang-code}.properties`. For example, the Spanish locale file would be `values_es.properties`
3. Translate the strings and submit it back to us (either via Discord, or a pull request to this repo)

> **NOTE**: Variables use `{N}` where `N` is the zero-indexed order it is passed through in code.
> 
> In English, these are always in order `0, 1, 2... 5`. Some languages may be out of order to maintain the correct variables in the locale `1, 0, 3, 2... 5`

