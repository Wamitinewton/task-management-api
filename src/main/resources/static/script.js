const API_BASE_URL = 'http://localhost:8080';

const state = {
    accessToken: null,
    refreshToken: null,
    user: null
};

document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
    setupEventListeners();
});

function initializeApp() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    const refreshToken = urlParams.get('refreshToken');
    const error = urlParams.get('error');

    if (error) {
        showError(error);
        return;
    }

    if (token && refreshToken) {
        state.accessToken = token;
        state.refreshToken = refreshToken;
        window.history.replaceState({}, document.title, window.location.pathname);
        fetchUserData();
    } else {
        const storedToken = sessionStorage.getItem('accessToken');
        const storedRefreshToken = sessionStorage.getItem('refreshToken');

        if (storedToken && storedRefreshToken) {
            state.accessToken = storedToken;
            state.refreshToken = storedRefreshToken;
            fetchUserData();
        }
    }
}

function setupEventListeners() {
    document.getElementById('google-login-btn').addEventListener('click', handleGoogleLogin);
    document.getElementById('refresh-token-btn').addEventListener('click', handleRefreshToken);
    document.getElementById('logout-btn').addEventListener('click', handleLogout);
}

function handleGoogleLogin() {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
}

async function fetchUserData() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
            headers: {
                'Authorization': `Bearer ${state.accessToken}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to fetch user data');
        }

        const userData = await response.json();
        state.user = userData;

        sessionStorage.setItem('accessToken', state.accessToken);
        sessionStorage.setItem('refreshToken', state.refreshToken);

        displayUserData();
    } catch (error) {
        showError('Failed to load user data. Please login again.');
        handleLogout();
    }
}

function displayUserData() {
    document.getElementById('user-name').textContent = state.user.name;
    document.getElementById('user-email').textContent = state.user.email;
    document.getElementById('user-provider').textContent = state.user.provider;
    document.getElementById('user-avatar').src = state.user.profilePictureUrl || 'https://via.placeholder.com/64';
    document.getElementById('access-token').value = state.accessToken;
    document.getElementById('refresh-token').value = state.refreshToken;

    document.getElementById('login-section').classList.add('hidden');
    document.getElementById('user-section').classList.remove('hidden');
    document.getElementById('error-section').classList.add('hidden');
}

async function handleRefreshToken() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/refresh?refreshToken=${state.refreshToken}`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error('Token refresh failed');
        }

        const data = await response.json();
        state.accessToken = data.accessToken;
        state.refreshToken = data.refreshToken;
        state.user = data.user;

        sessionStorage.setItem('accessToken', state.accessToken);
        sessionStorage.setItem('refreshToken', state.refreshToken);

        displayUserData();
        showSuccess();
    } catch (error) {
        showError('Failed to refresh token. Please login again.');
        handleLogout();
    }
}

function handleLogout() {
    state.accessToken = null;
    state.refreshToken = null;
    state.user = null;

    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');

    document.getElementById('login-section').classList.remove('hidden');
    document.getElementById('user-section').classList.add('hidden');
    document.getElementById('error-section').classList.add('hidden');
}

function copyToken(elementId) {
    const input = document.getElementById(elementId);
    input.select();
    input.setSelectionRange(0, 99999);

    navigator.clipboard.writeText(input.value).then(() => {
        const button = event.currentTarget;
        const originalHTML = button.innerHTML;
        button.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>';

        setTimeout(() => {
            button.innerHTML = originalHTML;
        }, 1500);
    });
}

function showError(message) {
    document.getElementById('error-message').textContent = message;
    document.getElementById('error-section').classList.remove('hidden');
    document.getElementById('login-section').classList.remove('hidden');
    document.getElementById('user-section').classList.add('hidden');
}

function showSuccess() {
    const refreshBtn = document.getElementById('refresh-token-btn');
    const originalText = refreshBtn.textContent;
    refreshBtn.textContent = 'Token Refreshed!';
    refreshBtn.style.background = '#48bb78';

    setTimeout(() => {
        refreshBtn.textContent = originalText;
        refreshBtn.style.background = '';
    }, 2000);
}