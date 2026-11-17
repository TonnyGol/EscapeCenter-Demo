import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, cleanup } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import axios from 'axios'
import RoomDetail from './RoomDetail'

vi.mock('axios')

const ROOM_ID = 'narcos'
const ROOM_NAME = 'נרקוס'
const PRICES = [
    { fromParticipants: 3, pricePerParticipant: 170, discountedPrice: 170 },
    { fromParticipants: 4, pricePerParticipant: 140, discountedPrice: 130 },
    { fromParticipants: 6, pricePerParticipant: 120, discountedPrice: 110 },
]
const PRICES_FAILED_MESSAGE = /לא הצלחנו לטעון את המחירון/

const showRoom = () => render(
    <MemoryRouter initialEntries={[`/rooms/${ROOM_ID}`]}>
        <Routes><Route path="/rooms/:id" element={<RoomDetail />} /></Routes>
    </MemoryRouter>
)

describe('Room detail page', () => {
    afterEach(() => {
        cleanup()
        vi.resetAllMocks()
    })

    it('shows the price table of the room from the server, the last tier as "and up"', async () => {
        axios.get.mockResolvedValue({ data: [{ name: 'Other', prices: [] }, { name: ROOM_NAME, minParticipants: 3, capacity: 9, durationMinutes: 75, prices: PRICES }] })

        showRoom()

        expect(await screen.findByText('6 ומעלה')).toBeInTheDocument()
        expect(screen.getByText('140 ₪ למשתתף')).toBeInTheDocument()
        expect(screen.getByText('110 ₪ למשתתף')).toBeInTheDocument()
        expect(screen.getByText('3-9')).toBeInTheDocument()
        expect(screen.getByText('75 דקות')).toBeInTheDocument()
    })

    it('says the prices could not be loaded when the server does not answer', async () => {
        axios.get.mockRejectedValue(new Error('offline'))

        showRoom()

        expect(await screen.findByText(PRICES_FAILED_MESSAGE)).toBeInTheDocument()
    })
})
