(function () {
  "use strict";

  let metadata = null;
  const selectedDeps = new Set();
  let packageNameTouched = false;
  let nameTouched = false;

  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => Array.from(document.querySelectorAll(sel));

  function selectedBuildTool() {
    return $$('input[name="buildTool"]').find((r) => r.checked).value;
  }
  function selectedLanguage() {
    return $$('input[name="language"]').find((r) => r.checked).value;
  }
  function selectedPackaging() {
    return $$('input[name="packaging"]').find((r) => r.checked).value;
  }
  function selectedBootVersion() {
    const r = $$('input[name="bootVersion"]').find((r) => r.checked);
    return r ? r.value : null;
  }
  function bootVersionInfo(version) {
    return metadata.bootVersions.find((b) => b.version === version);
  }

  function renderBootVersions() {
    const container = $("#boot-version-list");
    container.innerHTML = "";
    const majors = [3, 2];
    let firstRadio = null;
    let recommendedRadio = null;

    for (const major of majors) {
      const versions = metadata.bootVersions.filter((b) => b.major === major);
      if (versions.length === 0) continue;

      const label = document.createElement("div");
      label.className = "boot-version-major-label";
      label.textContent = major + ".x";
      container.appendChild(label);

      for (const bv of versions) {
        const wrapper = document.createElement("label");
        const input = document.createElement("input");
        input.type = "radio";
        input.name = "bootVersion";
        input.value = bv.version;
        input.addEventListener("change", onBootVersionChange);

        if (!firstRadio) firstRadio = input;
        if (bv.recommended) recommendedRadio = input;

        wrapper.appendChild(input);
        wrapper.appendChild(document.createTextNode(bv.version));
        if (bv.recommended) {
          const badge = document.createElement("span");
          badge.className = "badge-recommended";
          badge.textContent = "recommended";
          wrapper.appendChild(badge);
        }
        container.appendChild(wrapper);
      }
    }

    (recommendedRadio || firstRadio).checked = true;
    onBootVersionChange();
  }

  function onBootVersionChange() {
    const version = selectedBootVersion();
    const info = bootVersionInfo(version);
    const select = $("#javaVersion");
    const previous = select.value;
    select.innerHTML = "";
    for (const jv of info.javaVersions) {
      const opt = document.createElement("option");
      opt.value = jv;
      opt.textContent = "Java " + jv;
      select.appendChild(opt);
    }
    if (info.javaVersions.includes(previous)) {
      select.value = previous;
    } else {
      select.value = info.javaVersions[info.javaVersions.length - 1];
    }
    refreshDependencyAvailability();
  }

  function refreshDependencyAvailability() {
    const version = selectedBootVersion();
    const info = bootVersionInfo(version);
    const major = info.major;
    $$(".dep-item").forEach((item) => {
      const id = item.dataset.id;
      const available = item.dataset["boot" + major] === "true";
      const checkbox = item.querySelector("input");
      checkbox.disabled = !available;
      item.classList.toggle("unavailable", !available);
      if (!available && checkbox.checked) {
        checkbox.checked = false;
        selectedDeps.delete(id);
      }
    });
    renderChips();
  }

  function renderDependencyGroups() {
    const container = $("#dep-groups");
    container.innerHTML = "";
    for (const group of metadata.dependencyGroups) {
      const groupEl = document.createElement("div");
      groupEl.className = "dep-group";
      groupEl.dataset.group = group.name;

      const h3 = document.createElement("h3");
      h3.textContent = group.name;
      groupEl.appendChild(h3);

      for (const dep of group.dependencies) {
        const item = document.createElement("label");
        item.className = "dep-item";
        item.dataset.id = dep.id;
        item.dataset.name = dep.name.toLowerCase();
        item.dataset.desc = dep.description.toLowerCase();
        item.dataset.boot2 = String(dep.boot2);
        item.dataset.boot3 = String(dep.boot3);

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = dep.id;
        checkbox.addEventListener("change", () => {
          if (checkbox.checked) selectedDeps.add(dep.id);
          else selectedDeps.delete(dep.id);
          renderChips();
        });

        const textWrap = document.createElement("span");
        const nameEl = document.createElement("span");
        nameEl.className = "dep-name";
        nameEl.textContent = dep.name;
        const descEl = document.createElement("span");
        descEl.className = "dep-desc";
        descEl.textContent = dep.description;

        textWrap.appendChild(nameEl);
        textWrap.appendChild(document.createElement("br"));
        textWrap.appendChild(descEl);

        item.appendChild(checkbox);
        item.appendChild(textWrap);
        groupEl.appendChild(item);
      }

      container.appendChild(groupEl);
    }
  }

  function renderChips() {
    const row = $("#selected-chips");
    row.innerHTML = "";
    for (const id of selectedDeps) {
      const dep = findDep(id);
      if (!dep) continue;
      const chip = document.createElement("span");
      chip.className = "chip";
      chip.textContent = dep.name;
      const btn = document.createElement("button");
      btn.type = "button";
      btn.textContent = "×";
      btn.addEventListener("click", () => {
        selectedDeps.delete(id);
        const cb = document.querySelector('.dep-item[data-id="' + id + '"] input');
        if (cb) cb.checked = false;
        renderChips();
      });
      chip.appendChild(btn);
      row.appendChild(chip);
    }
  }

  function findDep(id) {
    for (const group of metadata.dependencyGroups) {
      const dep = group.dependencies.find((d) => d.id === id);
      if (dep) return dep;
    }
    return null;
  }

  function wireSearch() {
    $("#depSearch").addEventListener("input", (e) => {
      const q = e.target.value.trim().toLowerCase();
      $$(".dep-group").forEach((groupEl) => {
        let anyVisible = false;
        Array.from(groupEl.querySelectorAll(".dep-item")).forEach((item) => {
          const match = !q || item.dataset.name.includes(q) || item.dataset.desc.includes(q);
          item.classList.toggle("hidden", !match);
          if (match) anyVisible = true;
        });
        groupEl.classList.toggle("hidden", !anyVisible);
      });
    });
  }

  function wireMetadataAutofill() {
    const groupId = $("#groupId");
    const artifactId = $("#artifactId");
    const name = $("#name");
    const packageName = $("#packageName");

    name.addEventListener("input", () => { nameTouched = true; });
    packageName.addEventListener("input", () => { packageNameTouched = true; });

    function refresh() {
      if (!nameTouched) name.value = artifactId.value;
      if (!packageNameTouched) {
        packageName.value = (groupId.value + "." + artifactId.value)
          .toLowerCase()
          .replace(/[^a-z0-9.]/g, "");
      }
    }
    groupId.addEventListener("input", refresh);
    artifactId.addEventListener("input", refresh);
  }

  function wireGenerate() {
    $("#generate-btn").addEventListener("click", () => {
      const status = $("#status-message");
      status.textContent = "";

      const groupId = $("#groupId").value.trim();
      const artifactId = $("#artifactId").value.trim();
      if (!groupId || !artifactId) {
        status.textContent = "Group and Artifact are required.";
        return;
      }

      const params = new URLSearchParams({
        buildTool: selectedBuildTool(),
        language: selectedLanguage(),
        packaging: selectedPackaging(),
        bootVersion: selectedBootVersion(),
        javaVersion: $("#javaVersion").value,
        groupId: groupId,
        artifactId: artifactId,
        name: $("#name").value.trim() || artifactId,
        description: $("#description").value.trim(),
        packageName: $("#packageName").value.trim(),
        dependencies: Array.from(selectedDeps).join(","),
      });

      window.location.href = "/starter.zip?" + params.toString();
    });
  }

  fetch("/api/metadata")
    .then((r) => r.json())
    .then((data) => {
      metadata = data;
      renderBootVersions();
      renderDependencyGroups();
      refreshDependencyAvailability();
      wireSearch();
      wireMetadataAutofill();
      wireGenerate();
    })
    .catch((err) => {
      $("#status-message").textContent = "Failed to load metadata: " + err;
    });
})();
