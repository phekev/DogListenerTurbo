"use strict";

const statusRefreshMillis = 2_000;
const confidenceRefreshMillis = 2_000;
const historyRefreshMillis = 10_000;
const timelineRefreshMillis = 10_000;

let responseTimeline = {
    windowStart: null,
    windowEnd: null,
    responses: []
};

const responseTimelineCanvas =
    document.getElementById(
        "response-timeline-chart"
    );

const responseTimelineEmpty =
    document.getElementById(
        "response-timeline-empty"
    );
let confidenceSamples = [];

const connectionIndicator =
    document.getElementById(
        "connection-indicator"
    );

const confidenceCanvas =
    document.getElementById(
        "confidence-chart"
    );

const confidenceEmpty =
    document.getElementById(
        "confidence-empty"
    );
async function refreshResponseTimeline() {
    try {
        responseTimeline =
            await fetchJson(
                "/api/responses/timeline?hours=24"
            );

        const responseCount =
            responseTimeline.responses.length;

        document.getElementById(
            "timeline-count"
        ).textContent =
            responseCount === 1
                ? "1 response"
                : `${responseCount} responses`;

        drawResponseTimeline();

    } catch (error) {
        console.error(
            "Unable to refresh response timeline.",
            error
        );
    }
}
function drawResponseTimeline() {
    const canvas =
        responseTimelineCanvas;

    const rectangle =
        canvas.getBoundingClientRect();

    const pixelRatio =
        window.devicePixelRatio || 1;

    canvas.width =
        Math.max(
            1,
            Math.floor(
                rectangle.width * pixelRatio
            )
        );

    canvas.height =
        Math.max(
            1,
            Math.floor(
                rectangle.height * pixelRatio
            )
        );

    const context =
        canvas.getContext("2d");

    context.scale(
        pixelRatio,
        pixelRatio
    );

    const width =
        rectangle.width;

    const height =
        rectangle.height;

    context.clearRect(
        0,
        0,
        width,
        height
    );

    const windowStart =
        new Date(
            responseTimeline.windowStart
        ).getTime();

    const windowEnd =
        new Date(
            responseTimeline.windowEnd
        ).getTime();

    if (!Number.isFinite(windowStart)
        || !Number.isFinite(windowEnd)
        || windowEnd <= windowStart) {

        return;
    }

    const responses =
        responseTimeline.responses || [];

    responseTimelineEmpty.hidden =
        responses.length > 0;

    const computedStyle =
        getComputedStyle(
            document.documentElement
        );

    const borderColour =
        computedStyle
            .getPropertyValue("--border")
            .trim();

    const textColour =
        computedStyle
            .getPropertyValue("--text-muted")
            .trim();

    const firstColour =
        computedStyle
            .getPropertyValue("--accent")
            .trim();

    const secondColour =
        computedStyle
            .getPropertyValue("--warning")
            .trim();

    const prolongedColour =
        computedStyle
            .getPropertyValue("--danger")
            .trim();

    const padding = {
        top: 20,
        right: 20,
        bottom: 42,
        left: 95
    };

    const plotWidth =
        width
        - padding.left
        - padding.right;

    const plotHeight =
        height
        - padding.top
        - padding.bottom;

    const lanes = [
        {
            level: "FIRST",
            label: "First",
            colour: firstColour
        },
        {
            level: "SECOND",
            label: "Second",
            colour: secondColour
        },
        {
            level: "PROLONGED",
            label: "Prolonged",
            colour: prolongedColour
        }
    ];

    context.font =
        "12px system-ui";

    context.lineWidth = 1;

    /*
     * Draw six four-hour divisions over
     * the complete 24-hour period.
     */
    const divisionCount = 6;

    for (let index = 0;
         index <= divisionCount;
         index++) {

        const ratio =
            index / divisionCount;

        const x =
            padding.left
            + ratio * plotWidth;

        const timestamp =
            windowStart
            + ratio
            * (windowEnd - windowStart);

        context.strokeStyle =
            borderColour;

        context.beginPath();

        context.moveTo(
            x,
            padding.top
        );

        context.lineTo(
            x,
            padding.top + plotHeight
        );

        context.stroke();

        const label =
            new Intl.DateTimeFormat(
                undefined,
                {
                    weekday: "short",
                    hour: "2-digit",
                    minute: "2-digit"
                }
            ).format(
                new Date(timestamp)
            );

        context.fillStyle =
            textColour;

        context.textBaseline =
            "top";

        if (index === 0) {
            context.textAlign =
                "left";

        } else if (index === divisionCount) {
            context.textAlign =
                "right";

        } else {
            context.textAlign =
                "center";
        }

        context.fillText(
            label,
            x,
            padding.top
            + plotHeight
            + 10
        );
    }

    const laneHeight =
        plotHeight / lanes.length;

    lanes.forEach(
        (lane, index) => {
            const y =
                padding.top
                + laneHeight
                * (index + 0.5);

            context.strokeStyle =
                borderColour;

            context.beginPath();

            context.moveTo(
                padding.left,
                y
            );

            context.lineTo(
                padding.left
                + plotWidth,
                y
            );

            context.stroke();

            context.fillStyle =
                lane.colour;

            context.textAlign =
                "right";

            context.textBaseline =
                "middle";

            context.fillText(
                lane.label,
                padding.left - 12,
                y
            );
        }
    );

    responses.forEach(
        response => {
            const playedAt =
                new Date(
                    response.playedAt
                ).getTime();

            if (playedAt < windowStart
                || playedAt > windowEnd) {

                return;
            }

            const laneIndex =
                lanes.findIndex(
                    lane =>
                        lane.level
                        === response.responseLevel
                );

            if (laneIndex < 0) {
                return;
            }

            const ratio =
                (
                    playedAt - windowStart
                )
                / (
                    windowEnd - windowStart
                );

            const x =
                padding.left
                + ratio * plotWidth;

            const y =
                padding.top
                + laneHeight
                * (laneIndex + 0.5);

            context.fillStyle =
                lanes[laneIndex].colour;

            context.beginPath();

            context.arc(
                x,
                y,
                5,
                0,
                Math.PI * 2
            );

            context.fill();

            /*
             * Add a small vertical marker to make
             * closely grouped responses easier to see.
             */
            context.strokeStyle =
                lanes[laneIndex].colour;

            context.lineWidth = 2;

            context.beginPath();

            context.moveTo(
                x,
                y - 10
            );

            context.lineTo(
                x,
                y + 10
            );

            context.stroke();

            context.lineWidth = 1;
        }
    );
}
async function fetchJson(url) {
    const response = await fetch(
        url,
        {
            cache: "no-store"
        }
    );

    if (!response.ok) {
        throw new Error(
            `Request failed: ${response.status}`
        );
    }

    return response.json();
}

function markConnected() {
    connectionIndicator.textContent =
        "Connected";

    connectionIndicator.className =
        "connection-indicator connected";
}

function markDisconnected() {
    connectionIndicator.textContent =
        "Disconnected";

    connectionIndicator.className =
        "connection-indicator disconnected";
}

function formatUptime(milliseconds) {
    const totalSeconds =
        Math.floor(milliseconds / 1000);

    const days =
        Math.floor(totalSeconds / 86_400);

    const hours =
        Math.floor(
            (totalSeconds % 86_400) / 3_600
        );

    const minutes =
        Math.floor(
            (totalSeconds % 3_600) / 60
        );

    const seconds =
        totalSeconds % 60;

    const parts = [];

    if (days > 0) {
        parts.push(`${days}d`);
    }

    if (hours > 0 || days > 0) {
        parts.push(`${hours}h`);
    }

    if (minutes > 0 || hours > 0 || days > 0) {
        parts.push(`${minutes}m`);
    }

    parts.push(`${seconds}s`);

    return parts.join(" ");
}

function formatTime(value) {
    return new Intl.DateTimeFormat(
        undefined,
        {
            dateStyle: "medium",
            timeStyle: "medium"
        }
    ).format(new Date(value));
}



async function refreshConfidence() {
    try {
        confidenceSamples =
            await fetchJson(
                "/api/confidence?minutes=10"
            );

        const latest =
            confidenceSamples.at(-1);

        document.getElementById(
            "current-confidence"
        ).textContent =
            latest
                ? `${(
                    latest.confidence * 100
                ).toFixed(1)}%`
                : "—";

        drawConfidenceGraph();

    } catch (error) {
        console.error(
            "Unable to refresh confidence.",
            error
        );
    }
}
async function refreshStatus() {
    try {
        const status =
            await fetchJson("/api/status");

        document.getElementById(
            "application-status"
        ).textContent =
            status.applicationStatus;

        document.getElementById(
            "detection-status"
        ).textContent =
            status.detectionRunning
                ? "Running"
                : "Stopped";

        document.getElementById(
            "microphone-status"
        ).textContent =
            status.microphoneRunning
                ? "Running"
                : "Stopped";

        document.getElementById(
            "liveness-status"
        ).textContent =
            status.liveness;

        document.getElementById(
            "readiness-status"
        ).textContent =
            status.readiness;

        document.getElementById(
            "uptime"
        ).textContent =
            formatUptime(
                status.uptimeMillis
            );

        document.getElementById(
            "confidence-threshold"
        ).textContent =
            formatConfidence(
                status.confidenceThreshold
            );

        document.getElementById(
            "status-latest-confidence"
        ).textContent =
            formatConfidence(
                status.latestConfidence
            );

        document.getElementById(
            "last-audio-chunk"
        ).textContent =
            formatOptionalTime(
                status.lastAudioChunkAt
            );

        document.getElementById(
            "last-prediction"
        ).textContent =
            formatOptionalTime(
                status.lastPredictionAt
            );

        document.getElementById(
            "prediction-age"
        ).textContent =
            formatAge(
                status.latestPredictionAgeMillis
            );

        document.getElementById(
            "last-bark"
        ).textContent =
            formatOptionalTime(
                status.lastBarkAt
            );

        document.getElementById(
            "status-updated"
        ).textContent =
            `Updated ${formatTime(
                status.serverTime
            )}`;

        markConnected();

    } catch (error) {
        console.error(
            "Unable to refresh status.",
            error
        );

        markDisconnected();
    }
}
function drawConfidenceGraph() {
    const canvas =
        confidenceCanvas;

    const rectangle =
        canvas.getBoundingClientRect();

    const pixelRatio =
        window.devicePixelRatio || 1;

    canvas.width =
        Math.max(
            1,
            Math.floor(
                rectangle.width * pixelRatio
            )
        );

    canvas.height =
        Math.max(
            1,
            Math.floor(
                rectangle.height * pixelRatio
            )
        );

    const context =
        canvas.getContext("2d");

    context.scale(
        pixelRatio,
        pixelRatio
    );

    const width = rectangle.width;
    const height = rectangle.height;

    context.clearRect(
        0,
        0,
        width,
        height
    );

    confidenceEmpty.hidden =
        confidenceSamples.length > 0;

    if (confidenceSamples.length === 0) {
        return;
    }

    const computedStyle =
        getComputedStyle(document.documentElement);

    const borderColour =
        computedStyle
            .getPropertyValue("--border")
            .trim();

    const textColour =
        computedStyle
            .getPropertyValue("--text-muted")
            .trim();

    const accentColour =
        computedStyle
            .getPropertyValue("--accent")
            .trim();

    const padding = {
        top: 15,
        right: 15,
        bottom: 30,
        left: 48
    };

    const plotWidth =
        width
        - padding.left
        - padding.right;

    const plotHeight =
        height
        - padding.top
        - padding.bottom;

    context.font =
        "12px system-ui";

    context.strokeStyle =
        borderColour;

    context.fillStyle =
        textColour;

    context.lineWidth = 1;

    for (let percentage = 0;
         percentage <= 100;
         percentage += 25) {

        const y =
            padding.top
            + plotHeight
            - (
                percentage / 100
            ) * plotHeight;

        context.beginPath();

        context.moveTo(
            padding.left,
            y
        );

        context.lineTo(
            padding.left + plotWidth,
            y
        );

        context.stroke();

        context.fillText(
            `${percentage}%`,
            4,
            y + 4
        );
    }

    const firstTime =
        new Date(
            confidenceSamples[0].recordedAt
        ).getTime();

    const lastTime =
        new Date(
            confidenceSamples.at(-1).recordedAt
        ).getTime();

    const timeRange =
        Math.max(
            1,
            lastTime - firstTime
        );

    context.strokeStyle =
        accentColour;

    context.lineWidth = 2;

    context.beginPath();

    confidenceSamples.forEach(
        (sample, index) => {
            const timestamp =
                new Date(
                    sample.recordedAt
                ).getTime();

            const x =
                padding.left
                + (
                    (timestamp - firstTime)
                    / timeRange
                ) * plotWidth;

            const y =
                padding.top
                + plotHeight
                - sample.confidence
                * plotHeight;

            if (index === 0) {
                context.moveTo(x, y);
            } else {
                context.lineTo(x, y);
            }
        }
    );

    context.stroke();

    context.fillStyle =
        textColour;

    context.fillText(
        new Date(firstTime)
            .toLocaleTimeString(),
        padding.left,
        height - 5
    );

    const finalTimeLabel =
        new Date(lastTime)
            .toLocaleTimeString();

    const finalLabelWidth =
        context.measureText(
            finalTimeLabel
        ).width;

    context.fillText(
        finalTimeLabel,
        width
        - padding.right
        - finalLabelWidth,
        height - 5
    );
}

async function refreshHistory() {
    try {
        const [
            statistics,
            history
        ] = await Promise.all([
            fetchJson(
                "/api/responses/statistics"
            ),

            fetchJson(
                "/api/responses?limit=50"
            )
        ]);

        document.getElementById(
            "first-total"
        ).textContent =
            statistics.firstResponses;

        document.getElementById(
            "second-total"
        ).textContent =
            statistics.secondResponses;

        document.getElementById(
            "prolonged-total"
        ).textContent =
            statistics.prolongedResponses;

        document.getElementById(
            "overall-total"
        ).textContent =
            statistics.overallResponses;

        renderHistory(history);

        document.getElementById(
            "history-updated"
        ).textContent =
            `Updated ${new Date()
                .toLocaleTimeString()}`;

    } catch (error) {
        console.error(
            "Unable to refresh response history.",
            error
        );
    }
}

function renderHistory(history) {
    const tableBody =
        document.getElementById(
            "history-body"
        );

    tableBody.replaceChildren();

    if (history.length === 0) {
        const row =
            document.createElement("tr");

        const cell =
            document.createElement("td");

        cell.colSpan = 3;
        cell.textContent =
            "No responses have been recorded.";

        row.appendChild(cell);
        tableBody.appendChild(row);

        return;
    }

    history.forEach(entry => {
        const row =
            document.createElement("tr");

        const timeCell =
            document.createElement("td");

        timeCell.textContent =
            formatTime(entry.playedAt);

        const levelCell =
            document.createElement("td");

        levelCell.textContent =
            entry.responseLevel;

        const soundCell =
            document.createElement("td");

        soundCell.textContent =
            entry.filename;

        soundCell.title =
            entry.soundFile;

        row.append(
            timeCell,
            levelCell,
            soundCell
        );

        tableBody.appendChild(row);
    });
}

window.addEventListener(
    "resize",
    () => {
        drawConfidenceGraph();
        drawResponseTimeline();
    }
);
setInterval(
    refreshResponseTimeline,
    timelineRefreshMillis
);

refreshStatus();
refreshConfidence();
refreshResponseTimeline();
refreshHistory();

setInterval(
    refreshStatus,
    statusRefreshMillis
);
function formatOptionalTime(value) {
    if (!value) {
        return "Never";
    }

    return formatTime(value);
}

function formatConfidence(value) {
    if (value === null
        || value === undefined) {

        return "—";
    }

    return `${(
        value * 100
    ).toFixed(1)}%`;
}

function formatAge(milliseconds) {
    if (milliseconds === null
        || milliseconds === undefined) {

        return "No prediction yet";
    }

    return `${formatUptime(milliseconds)} ago`;
}
setInterval(
    refreshConfidence,
    confidenceRefreshMillis
);

setInterval(
    refreshHistory,
    historyRefreshMillis
);