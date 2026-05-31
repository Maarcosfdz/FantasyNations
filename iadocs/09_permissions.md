# Permissions

## Main rule

Agents have full permission to work inside the repository folder:

```txt
FantasyNation/

Inside this folder, agents may:

read files;
create files;
edit files;
delete files;
move files;
rename files;
create folders;
delete folders;
run project commands;
install dependencies when justified;
use browsers or local tools if needed;
inspect logs and outputs;
run tests, linters and builds.

Agents do not need to ask for permission for normal development work inside FantasyNation/.

Allowed inside FantasyNation/

Agents may freely work with:

source code
tests
docs
configuration files
Docker files
package files
lock files
scripts
public assets
mock data
fixtures
database migrations
README files
iadocs/response.md
arevisar.md

They may create, edit or delete files when needed for the assigned task.

Not allowed outside FantasyNation/

Agents must not modify, delete, move or rename files outside the repository.

Forbidden outside FantasyNation/:

delete folders
edit personal files
move personal files
rename personal files
change system configuration
delete desktop/downloads/documents folders
modify unrelated projects
modify operating system files
modify SSH keys
modify global git config
modify browser profiles
modify password managers
modify cloud credentials

Besides you have all the pemisions with database and docker