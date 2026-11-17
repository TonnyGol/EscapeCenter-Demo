import { useEffect, useState } from 'react';
import axios from 'axios';
import { API_URL } from './api';

/** The rooms as the server describes them: null while loading, an empty list when they could not be loaded. */
export const useApiRooms = () => {
    const [rooms, setRooms] = useState(null);

    useEffect(() => {
        axios.get(`${API_URL}/api/rooms`)
            .then(res => setRooms(res.data))
            .catch(() => setRooms([]));
    }, []);

    return rooms;
};

export const playersLabel = apiRoom => `${apiRoom.minParticipants}-${apiRoom.capacity}`;
