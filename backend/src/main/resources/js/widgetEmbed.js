(async function () {
  /**
   * Parses the base URL from the currentScript's URL
   * @type {string|null}
   */
  const scriptBaseUrl = document.currentScript
    ? new URL("../..", document.currentScript.src).href.replace(/\/+$/, "")
    : null;

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
   * @param config {Element|null}
   * @param names {Array.<String>}
   * @param out {Array.<Array.<String>>}
   */
  function collectParams(element, config, names, out) {
    names.forEach((n) => {
      let value = element.dataset[n];
      if (value) {
        out.push([n, value]);
      }
      else if (config) {
        value = config.dataset[n];
        if (value) {
          out.push([n, value]);
        }
      }
    })
  }

  const config = document.getElementById("vibin-config");

  const baseUrl = config?.dataset?.baseUrl?.replace(/\/+$/, "") ?? scriptBaseUrl;
  if (!baseUrl) {
    console.error("Couldn't determine the base URL for the Vibin' widgets! Set it with data-base-url on #vibin-config.");
    return;
  }

  if (config?.dataset?.css !== "false") {
    try {
      await loadCss(baseUrl);
    }
    catch (e) {
      console.error("An error occurred while loading the widget CSS.", e);
      return;
    }
  }

  const widgets = document.querySelectorAll(".vibin-widget[data-widget-id]");

  await Promise.all(Array.from(widgets).map(async (widget) => {
    try {

      if (widget.dataset.status === "loading" || widget.dataset.status === "loaded") {
        return;
      }

      widget.dataset.status = "loading";

      /** @type {Array.<Array.<String>>} */
      const params = [];
      params.push(["baseUrl", baseUrl]);

      const id = widget.attributes["data-widget-id"]?.value;
      if (!id) {
        console.error("No id defined for widget", widget);
        return;
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
      if (!response.ok) {
        console.error(`An error occurred while loading widget ${id}: ${response.status} - ${response.statusText}`, await response.text())
        return;
      }

      widget.innerHTML = await response.text();
      activateScripts(widget);
      widget.dataset.status = "loaded";
    }
    catch (e) {
      console.error("An error occurred while creating the widget.", widget, e);
    }
  }));
})();