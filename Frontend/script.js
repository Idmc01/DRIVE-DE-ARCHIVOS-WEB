let currentUser = "";
let currentPath = "/";

function enterDrive() {
  const username = document.getElementById("username").value.trim();
  if (!username) return alert("Ingrese un nombre de usuario");
  currentUser = username;
  document.getElementById("driveUI").style.display = "block";
  document.getElementById("userInput").style.display = "none";
  loadDirectory();
}

function loadDirectory() {
  document.getElementById("currentPath").innerText = currentPath;
  const fileView = document.getElementById("fileView");
  fileView.innerHTML = "";

  // Aquí deberías hacer una llamada al backend con fetch para obtener archivos
  // Por ahora simulamos:
  const fakeData = [
    { name: "documento.txt", type: "file" },
    { name: "trabajos", type: "folder" }
  ];

  fakeData.forEach(item => {
    const div = document.createElement("div");
    div.className = item.type === "file" ? "file" : "folder";
    div.innerText = item.name;
    div.onclick = () => {
      if (item.type === "folder") {
        currentPath += item.name + "/";
        loadDirectory();
      } else {
        alert("Visualizar archivo: " + item.name);
      }
    };
    fileView.appendChild(div);
  });
}

// Modal
let modalAction = "";
function createFolder() {
  modalAction = "folder";
  openModal("Crear Directorio");
}
function createFile() {
  modalAction = "file";
  openModal("Crear Archivo");
}
function openModal(title) {
  document.getElementById("modalTitle").innerText = title;
  document.getElementById("modal").classList.remove("hidden");
}
function closeModal() {
  document.getElementById("modal").classList.add("hidden");
  document.getElementById("modalInput").value = "";
  document.getElementById("modalText").value = "";
}
function confirmModal() {
  const name = document.getElementById("modalInput").value;
  const content = document.getElementById("modalText").value;
  closeModal();

  if (modalAction === "folder") {
    alert(`Crear carpeta "${name}" en ${currentPath}`);
    // fetch para crear carpeta
  } else {
    alert(`Crear archivo "${name}" con contenido:\n${content}`);
    // fetch para crear archivo
  }

  // Actualizar vista
  loadDirectory();
}
