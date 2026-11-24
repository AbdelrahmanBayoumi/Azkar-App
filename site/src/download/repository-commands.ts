export const repositoryCommands = {
  apt: "curl -fsSL https://jamhour1g.github.io/Azkar-App/keys/azkar-repo.asc | gpg --dearmor | sudo tee /usr/share/keyrings/azkar-repo.gpg >/dev/null\necho 'deb [signed-by=/usr/share/keyrings/azkar-repo.gpg] https://jamhour1g.github.io/Azkar-App/apt stable main' | sudo tee /etc/apt/sources.list.d/azkar.list\nsudo apt update\nsudo apt install azkar",
  dnf: "sudo rpm --import https://jamhour1g.github.io/Azkar-App/keys/azkar-repo.asc\nsudo tee /etc/yum.repos.d/azkar.repo >/dev/null <<'EOF'\n[azkar-stable]\nname=Azkar Stable\nbaseurl=https://jamhour1g.github.io/Azkar-App/rpm/stable/\nenabled=1\ngpgcheck=0\nrepo_gpgcheck=1\ngpgkey=https://jamhour1g.github.io/Azkar-App/keys/azkar-repo.asc\nEOF\nsudo dnf makecache\nsudo dnf install azkar",
};
