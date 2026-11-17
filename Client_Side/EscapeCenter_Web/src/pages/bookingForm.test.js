import { describe, expect, it } from 'vitest'
import { clampParticipants, participantOptions, pricePerPerson, totalPrice } from './bookingForm'

const ROOM = {
    capacity: 6,
    minParticipants: 3,
    prices: [
        { fromParticipants: 3, pricePerParticipant: 170, discountedPrice: 170 },
        { fromParticipants: 4, pricePerParticipant: 140, discountedPrice: 130 },
        { fromParticipants: 6, pricePerParticipant: 120, discountedPrice: 110 },
    ],
}
const ROOM_WITHOUT_PRICES = { ...ROOM, prices: [] }

describe('price of a group', () => {
    it('uses the tier with the largest start not above the group', () => {
        expect(pricePerPerson(ROOM, 3)).toBe(170)
        expect(pricePerPerson(ROOM, 5)).toBe(140)
        expect(pricePerPerson(ROOM, 6)).toBe(120)
    })

    it('charges a group below the first tier the first tier price', () => {
        expect(pricePerPerson(ROOM, 2)).toBe(170)
    })

    it('multiplies by the group size, also when the size comes from a form as text', () => {
        expect(totalPrice(ROOM, '5')).toBe(700)
    })

    it('has no price at all for a room without tiers, instead of a free game', () => {
        expect(pricePerPerson(ROOM_WITHOUT_PRICES, 4)).toBeNull()
        expect(totalPrice(ROOM_WITHOUT_PRICES, 4)).toBeNull()
    })
})

describe('group size choices', () => {
    it('go from the room minimum to its capacity', () => {
        expect(participantOptions(ROOM)).toEqual([3, 4, 5, 6])
    })

    it('pull a remembered size back into the range of the chosen room', () => {
        expect(clampParticipants(ROOM, 2)).toBe(3)
        expect(clampParticipants(ROOM, 12)).toBe(6)
        expect(clampParticipants(ROOM, '4')).toBe(4)
    })
})
