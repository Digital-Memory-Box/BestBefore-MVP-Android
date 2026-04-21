const CLIENT_ID = 'ALkAMYHptiNuZ5wq0viSlcF0BfWrSTD2';
const CLIENT_SECRET = 'Ypyx6Ne3AOtywN3WQHpdvdtbJozzrbFz';
const PLAYLIST_URL = 'https://soundcloud.com/ozan-guengoer-193834135/sets/bestbefore';

let cachedToken = null;
let tokenExpiresAt = 0;

async function getAccessToken() {
    if (cachedToken && Date.now() < tokenExpiresAt) {
        return cachedToken;
    }

    const params = new URLSearchParams();
    params.append('grant_type', 'client_credentials');
    params.append('client_id', CLIENT_ID);
    params.append('client_secret', CLIENT_SECRET);

    const response = await fetch('https://api.soundcloud.com/oauth2/token', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Accept': 'application/json'
        },
        body: params.toString()
    });

    if (!response.ok) {
        throw new Error(`Failed to get SoundCloud token: ${response.status} ${await response.text()}`);
    }

    const data = await response.json();
    cachedToken = data.access_token;
    // Data expires_in is in seconds
    tokenExpiresAt = Date.now() + ((data.expires_in - 60) * 1000); // 1 minute buffer
    return cachedToken;
}

export const getPlaylist = async (req, res) => {
    try {
        const token = await getAccessToken();
        
        // Resolve the playlist URL
        const resolveRes = await fetch(`https://api.soundcloud.com/resolve?url=${encodeURIComponent(PLAYLIST_URL)}`, {
            headers: { 'Authorization': `OAuth ${token}` }
        });

        if (!resolveRes.ok) {
            return res.status(resolveRes.status).json({ error: 'Failed to resolve playlist' });
        }

        const playlistData = await resolveRes.json();
        
        // Format the response for the Android app
        if (!playlistData.tracks) {
            return res.json({ title: playlistData.title, tracks: [] });
        }

        const tracks = playlistData.tracks.map(track => ({
            id: track.id,
            title: track.title,
            artist: track.user?.username || 'Unknown Artist',
            duration: track.duration,
            // Instead of direct stream_url, we route through our backend to inject OAuth header
            streamUrl: `/api/soundcloud/stream/${track.id}`,
            artworkUrl: track.artwork_url || playlistData.artwork_url
        }));

        res.json({
            title: playlistData.title,
            tracks
        });
    } catch (error) {
        console.error('Error fetching SoundCloud playlist:', error);
        res.status(500).json({ error: 'Internal Server Error' });
    }
};

export const streamTrack = async (req, res) => {
    try {
        const { trackId } = req.params;
        const token = await getAccessToken();
        
        // Use the native fetch to initiate the stream and pipe it back to the client
        const streamUrl = `https://api.soundcloud.com/tracks/${trackId}/stream`;
        
        const streamRes = await fetch(streamUrl, {
            headers: { 'Authorization': `OAuth ${token}` },
            redirect: 'manual'
        });

        if (streamRes.status === 302 || streamRes.status === 301) {
            // It gave us a redirect to the actual CDN MP3 url
            const location = streamRes.headers.get('location');
            if (location) {
                // We can let the Android app download from the CDN directly since the CDN URL
                // usually has a short-lived signature and doesn't explicitly require the Auth header!
                return res.redirect(location);
            }
        }

        if (!streamRes.ok) {
            return res.status(streamRes.status).send('Failed to stream track');
        }

        // If it didn't redirect, pipe it
        streamRes.headers.forEach((val, key) => {
            res.setHeader(key, val);
        });
        
        // Convert web readable stream to node stream
        const { Readable } = await import('stream');
        Readable.fromWeb(streamRes.body).pipe(res);

    } catch (error) {
        console.error('Error streaming track:', error);
        res.status(500).json({ error: 'Internal Server Error' });
    }
};
