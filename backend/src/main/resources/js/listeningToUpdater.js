const title = document.getElementById("listeningToTitle");
const artists = document.getElementById("listeningToArtists");
const state = document.getElementById("listeningToState");
const cover = document.getElementById("listeningToAlbumCover");
const progressBar = document.getElementById("listeningToProgress");
const position = document.getElementById("listeningToPosition");
const duraiton = document.getElementById("listeningToDuration");
const progressRow = document.getElementById("listeningToProgressRow");

const wsUrl = location.href.replace(/^http/, "ws") + "/listeningto/ws";

const ws = new WebSocket(wsUrl);
let pingInterval = undefined;

let progress = 0;

let lastData = null;

function updateDuration() {
    if (lastData.old) {
        progressRow.style.visibility = 'hidden';
    }
    else {
        progressRow.style.visibility = 'visible';

        if (progress === null || lastData.duration === null) {
            progressBar.classList.add('indeterminate');
            position.style.display = 'none';
            duraiton.style.display = 'none';
            return;
        }

        position.style.display = 'block';
        position.innerText = formatTime(progress);

        duraiton.style.display = 'block';
        duraiton.innerText = formatTime(lastData.duration / 1000);

        progressBar.classList.remove('indeterminate');
        progressBar.style.width = `${Math.min(100, Math.max(0, (progress * 1000 / lastData.duration * 100)))}%`;
    }
}

setInterval(() => {
    if (lastData !== null && progress !== null && lastData.state === 'PLAYING') {
        progress++;
        updateDuration();
    }
}, 1000);

function getRelativeDate(ts) {
    const now=  new Date();
    const date = new Date(ts * 1000);

    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

    const diff = Math.floor((today - targetDate) / (1000 * 60 * 60 * 24));

    if (diff === 0) return 'today';
    if (diff === 1) return 'yesterday';
    if (diff < 7) return `${diff} days ago`;
    if (diff < 30) return `${diff} weeks ago`;

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
    console.log("Error:", e)
});

ws.addEventListener("message", (m) => {
    console.log("Message:", m);

    if (typeof m.data === 'string') {
        if (m.data === "ping") return;

        let frame = JSON.parse(m.data)

        switch (frame.type) {
            case "state":
                title.innerText = frame.data.title;
                artists.innerText = frame.data.artists;

                if (frame.data.old) {
                    state.innerText = `⏹ Offline (last seen: ${getRelativeDate(frame.data.updated)})`
                }
                else {
                    let stateStr = undefined;

                    switch (frame.data.state) {
                        case "PLAYING":
                            stateStr = "▶ Playing";
                            break;
                        case "PAUSED":
                            stateStr = "⏸ Paused"
                            break;
                        case "STOPPED":
                            stateStr = "⏹ Stopped";
                            break;
                        default:
                            break;
                    }

                    if (stateStr !== undefined) {
                        state.innerText = stateStr;
                    }
                }

                if (frame.data.cover != null) {
                    cover.src = frame.data.cover;
                }
                cover.title = frame.data.album;

                progress = frame.data.position;
                lastData = frame.data;
                updateDuration();
                break;
        }
    }

});