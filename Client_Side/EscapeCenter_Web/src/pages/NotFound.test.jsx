import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import NotFound from './NotFound'

describe('NotFound', () => {
    it('names what is missing, links back and sets the page title', () => {
        render(<MemoryRouter><NotFound title="חדר לא נמצא" backTo="/rooms" backLabel="חזרה לרשימת החדרים" /></MemoryRouter>)

        expect(screen.getByRole('heading', { name: 'חדר לא נמצא' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'חזרה לרשימת החדרים' })).toHaveAttribute('href', '/rooms')
        expect(document.title).toBe('חדר לא נמצא | EscapeCenter')
    })
})
