async function loadCss(baseUrl) {
  return new Promise((resolve, reject) => {
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.type = "text/css";
    link.href = `${baseUrl}/api/widgets/styles`;
    link.onload = () => { resolve() };
    link.onerror = () => { reject() };
    document.head.appendChild(link);
  });
}

/**
 * Scripts inserted via innerHTML are never executed, so each one has to be replaced by a new copy.
 * @param element {Element}
 */
function activateScripts(element) {
  element.querySelectorAll("script").forEach((oldScript) => {
    const newScript = document.createElement("script");
    for (const attr of oldScript.attributes) {
      newScript.setAttribute(attr.name, attr.value);
    }
    newScript.textContent = oldScript.textContent;
    newScript.async = false;
    oldScript.replaceWith(newScript);
  });
}

/**
 * @param element {Element}
 * @param config {Element}
 * @param names {Array.<String>}
 * @param out {Array.<Array.<String>>}
 */
function collectParams(element, config, names, out) {
  names.forEach((n) => {
    let value = element.attributes[`data-${n.toLowerCase()}`]?.value;
    if (value) {
      out.push([n, value]);
    }
    else {
      value = config.attributes[`data-${n.toLowerCase()}`]?.value;
      if (value) {
        out.push([n, value]);
      }
    }
  })
}

(async function () {
  const config = document.getElementById("vibin-config");
  if (!config) {
    console.error("No element with id \"vibin-config\" found!");
    return;
  }

  const baseUrl = config.attributes["data-base-url"]?.value;
  if (!baseUrl) {
    console.error("No base URL was defined for the Vibin' widgets!");
    return;
  }

  if (config.attributes["data-css"]?.value !== "false") {
    await loadCss(baseUrl);
  }

  const widgets = document.querySelectorAll(".vibin-widget");
  for (const widget of widgets) {

    try {
      /** @type {Array.<Array.<String>>} */
      const params = [];
      params.push(["external", "true"]);

      const id = widget.attributes["data-widget-id"]?.value;
      if (!id) {
        console.error("No id defined for widget", widget);
        continue;
      }

      collectParams(widget, config, [
        "lang", // Language
        "bgColor", // Background color
        "fgColor", // Foreground (text) color
        "accentColor"
      ], params);

      const queryString = params.length === 0
        ? ""
        : `?${params.map((p) => `${p[0]}=${encodeURIComponent(p[1])}`).join('&')}`;

      const response = await fetch(`${baseUrl}/api/widgets/${id}${queryString}`);
      const html = await response.text();

      widget.innerHTML = html;
      activateScripts(widget);
    }
    catch (e) {
      console.error("An error occurred while creating the widget.", widget, e);
    }
  }
})();