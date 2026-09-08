# School of Sorcery - Council Admission & House Assignment Engine

A robust, deterministic backend system built in **Java 21** to process student applications for the School of Sorcery, paired with a modern web dashboard built with **Tailwind CSS**.

## Features

- **JSON Data Processing**: Reads applications and council rules using `GSON`.
- **Business Rule Enforcement**: Handles direct vetoes (banned families, age ranges, unacceptable weaknesses, deadlines) and invitation lists.
- **Deterministic Scoring & Ranking**: Calculates student scores and applies strict tie-breaking criteria (score descending, age ascending, family name, first name, and ID ascending) to guarantee identical outputs regardless of input line order.
- **House Assignment**: Dynamically assigns students to houses (*Lion*, *Serpent*, *Raven*, *Badger*) based on their virtues.
- **Interactive Web Dashboard**: Built with Tailwind CSS featuring real-time name searching and house filtering tabs for accepted students.

## Project Structure

```text
├── data/
│   ├── applications.json
│   └── council-rules.json
├── public/
│   └── index.html
├── src/
│   └── main/java/com/sorcery/
│       ├── Main.java
│       ├── Model.java
│       └── SorceryEngine.java
├── pom.xml
├── README.md
└── AI-NOTES.md
```

## How to Get and Run the Project

### 1. Clone the Repository

Open your terminal and run the following commands to clone the project and enter the directory:

**Bash**

```bash
git clone https://github.com/alvaro-f-g/The-School-of-Sorcery-.git
cd The-School-of-Sorcery-
```

### 2. Open in Visual Studio Code

1. Open **VS Code** and select **File > Open Folder...**, then choose the cloned project folder.
2. Ensure you have the **Extension Pack for Java** installed in VS Code.

### 3. Run the Backend Engine

1. Navigate to `src/main/java/com/sorcery/Main.java`.
2. Click the **"Run"** button or play icon located above the `main` method in the editor.
3. The Java 21 runtime will compile and process the files inside `data/`, automatically generating the processed results file at `public/results.json`.

### 4. Launch the Web Dashboard

1. Open the `public/index.html` file in your browser (or use the *Live Server* extension in VS Code).
2. Explore the council results dashboard, view accepted students grouped by house, filter by specific houses (*Lion*, *Serpent*, *Raven*, *Badger*), or use the real-time search bar to look up applicants instantly.
