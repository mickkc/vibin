/**
 * @typedef {Object} langDate
 * @property {string} today
 * @property {string} yesterday
 * @property {string} daysAgo
 * @property {string} weeksAgo
 */

/**
 * @typedef {Object} langState
 * @property {string} empty
 * @property {string} offline
 * @property {string} playing
 * @property {string} paused
 * @property {string} stopped
 */

/**
 * @typedef {Object} lang
 * @property {langDate} date Date localization strings.
 * @property {Object} state State localization strings.
 */

/**
 * @typedef {Object} trackState
 * @property {string} title Track title.
 * @property {string} artists Track artists, seperated by commas.
 * @property {string} album The track's album name.
 * @property {string|null} cover Url to the track's cover. Null if unchanged (same cover as before).
 * @property {string} state "PLAYING", "PAUSED", "STOPPED"
 * @property {number} updated Unix timestamp in seconds of the last time the session was updated.
 * @property {boolean} old True if thew session no longer exists (client that created it is offline)
 * @property {number} duration Track duration in milliseconds.
 * @property {number} position Current position / progress in seconds.
 */

const titleElement = document.getElementById("listeningToTitle");
const artistsElement = document.getElementById("listeningToArtists");
const stateElement = document.getElementById("listeningToState");
const coverElement = document.getElementById("listeningToAlbumCover");
const progressBarElement = document.getElementById("listeningToProgress");
const positionTextElement = document.getElementById("listeningToPosition");
const durationTextElement = document.getElementById("listeningToDuration");
const progressRow = document.getElementById("listeningToProgressRow");

const wsUrl = location.href.replace(/^http/, "ws") + "/listeningto/ws";

const ws = new WebSocket(wsUrl);
let pingInterval = undefined;

// The current progress in seconds
let currentProgress = 0;

// The last received track data
/** @type {trackState|null} */
let lastData = null;

function updateDuration() {
    if (lastData == null || lastData.old) {
        progressRow.style.visibility = 'hidden';
    }
    else {
        progressRow.style.visibility = 'visible';

        // Clamp current progrsess between 0 and the current track duration
        if (lastData.duration !== null) {
            currentProgress = Math.min(lastData.duration / 1000, Math.max(0, currentProgress));
        }

        if (currentProgress === null || lastData.duration === null) {
            progressBarElement.classList.add('indeterminate');
            positionTextElement.style.display = 'none';
            durationTextElement.style.display = 'none';
            return;
        }

        positionTextElement.style.display = 'block';
        positionTextElement.innerText = formatTime(currentProgress);

        durationTextElement.style.display = 'block';
        durationTextElement.innerText = formatTime(lastData.duration / 1000);

        progressBarElement.classList.remove('indeterminate');
        progressBarElement.style.width = `${Math.min(100, Math.max(0, (currentProgress * 1000 / lastData.duration * 100)))}%`;
    }
}

setInterval(() => {
    if (lastData !== null && currentProgress !== null && lastData.state === 'PLAYING') {
        currentProgress++;
        updateDuration();
    }
}, 1000);

function getRelativeDate(ts) {
    const now=  new Date();
    const date = new Date(ts * 1000);

    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

    const diff = Math.floor((today - targetDate) / (1000 * 60 * 60 * 24));

    if (diff === 0) return lang.date.today;
    if (diff === 1) return lang.date.yesterday;
    if (diff < 7) return lang.date.daysAgo.replace("{0}", diff.toString());
    if (diff < 30) return lang.date.weeksAgo.replace("{0}", diff.toString());

    return date.toLocaleDateString()
}

function formatTime(seconds) {
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);

    if (hours > 0) {
        return `${hours}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

ws.addEventListener("open", () => {
    console.log("Websocket connected.")
    pingInterval = setInterval(() => {
        ws.send("ping");
    }, 5000)
});

ws.addEventListener("error", (e) => {
    console.log("Error:", e);
});

ws.addEventListener("close", (e) => {
    console.log("Closing websocket conection...", e);
    if (pingInterval !== undefined) {
        clearInterval(pingInterval);
        pingInterval = undefined;
    }
})

ws.addEventListener("message", (m) => {
    console.log("Message:", m);

    if (typeof m.data === 'string') {
        if (m.data === "ping") return;

        let frame = JSON.parse(m.data)

        switch (frame.type) {
            case "state":
                /** @type {trackState} */
                const data = frame.data;

                titleElement.innerText = data.title;
                artistsElement.innerText = data.artists;

                if (data.old) {
                    stateElement.innerHTML = lang.state.offline.replace("{0}", getRelativeDate(frame.data.updated));
                }
                else {
                    let stateStr = undefined;

                    switch (data.state) {
                        case "PLAYING":
                            stateStr = lang.state.playing;
                            break;
                        case "PAUSED":
                            stateStr = lang.state.paused;
                            break;
                        case "STOPPED":
                            stateStr = lang.state.stopped;
                            break;
                        default:
                            break;
                    }

                    if (stateStr !== undefined) {
                        stateElement.innerHTML = stateStr;
                    }
                }

                if (data.cover != null) {
                    coverElement.src = data.cover;
                }
                coverElement.title = data.album;

                currentProgress = data.position;
                lastData = data;
                updateDuration();
                break;
            default:
                console.warn(`Unrecognized websocket frame type: ${frame.type}.`)
        }
    }

});