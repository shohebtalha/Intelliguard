import { expect, test } from '@playwright/test';

test('protected routes redirect unauthenticated users to login', async ({ page }) => {
    await page.goto('/cases');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
});

test('login form shows an error for invalid credentials', async ({ page }) => {
    await page.route('http://localhost:8080/api/auth/login', async route => {
        if (route.request().method() === 'OPTIONS') {
            await route.fulfill({
                status: 204,
                headers: {
                    'access-control-allow-origin': '*',
                    'access-control-allow-methods': 'POST, OPTIONS',
                    'access-control-allow-headers': 'content-type, authorization',
                },
            });
            return;
        }
        await route.fulfill({
            status: 401,
            headers: {
                'content-type': 'application/json',
                'access-control-allow-origin': '*',
                'access-control-allow-methods': 'POST, OPTIONS',
                'access-control-allow-headers': 'content-type, authorization',
            },
            body: JSON.stringify({ success: false, message: 'Invalid username or password' }),
        });
    });

    await page.goto('/login');
    await page.getByLabel('Username').fill('analyst');
    await page.getByLabel('Password').fill('wrong-password');
    await page.getByRole('button', { name: /sign in/i }).click();

    await expect(page.getByText(/invalid username or password/i)).toBeVisible();
});

test('authenticated shell exposes the case-management navigation', async ({ page }) => {
    await page.addInitScript(() => {
        localStorage.setItem('token', 'test-token');
        localStorage.setItem('username', 'analyst');
        localStorage.setItem('role', 'ANALYST');
        localStorage.setItem('tenantId', 'demo-bank');
    });
    await page.route('**/api/cases**', async route => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
                success: true,
                data: { content: [] },
                message: 'Fetched 0 cases',
            }),
        });
    });

    await page.goto('/cases');
    await expect(page.getByRole('navigation')).toBeVisible();
    await expect(page.getByText('Case Management')).toBeVisible();
    await expect(page.getByText('No cases in this queue')).toBeVisible();
});
