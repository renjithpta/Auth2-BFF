import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const env = Object.fromEntries(fs.readFileSync(path.join(root, '.env'), 'utf8')
  .trim().split('\n').map(line => {
    const i = line.indexOf('='); return [line.slice(0,i),line.slice(i+1)];
  }));

async function login(page, username) {
  await page.goto('/oauth2/authorization/aobp');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(env.DEMO_PASSWORD);
  await page.locator('#kc-login').click();
  await expect(page.getByRole('heading', { name: 'Operational flight board' })).toBeVisible();
}

test('operator loads independent remotes, updates a flight and logs out', async ({page,context}) => {
  const errors=[]; page.on('pageerror', error=>errors.push(error.message));
  await page.goto('/');
  await expect(page.getByRole('heading', {name:'Your airport operations, in one place.'})).toBeVisible();
  await login(page,'ops-t1');
  await expect(page.getByText('SN1234',{exact:true})).toBeVisible();
  await expect(page.getByText('BA456',{exact:true})).toHaveCount(0);
  await page.getByRole('button',{name:'Set boarding'}).click();
  await expect(page.getByText('BOARDING',{exact:true})).toBeVisible();
  expect((await context.cookies()).some(c=>c.name==='AOBPSESSION'&&c.httpOnly)).toBeTruthy();
  expect(await page.evaluate(()=>Object.keys(localStorage))).toEqual([]);
  await page.screenshot({path:path.join(root,'.runtime/ui-flight.png'),fullPage:true});
  await page.getByRole('button',{name:'Workflow operations',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Operational tasks'})).toBeVisible();
  await expect(page.getByText(/stand-change-001/)).toBeVisible();
  await page.getByRole('button',{name:'Sign out'}).click();
  await expect(page.getByRole('heading',{name:'Your airport operations, in one place.'})).toBeVisible();
  expect(errors).toEqual([]);
});

test('read-only T2 user sees own data and a meaningful workflow denial', async ({page}) => {
  const errors=[]; page.on('pageerror', error=>errors.push(error.message));
  await login(page,'ops-t2');
  await expect(page.getByText('BA456',{exact:true})).toBeVisible();
  await expect(page.getByText('SN1234',{exact:true})).toHaveCount(0);
  await expect(page.getByRole('button',{name:'Set boarding'})).toHaveCount(0);
  await page.getByRole('button',{name:'Workflow operations',exact:true}).click();
  await expect(page.getByRole('alert')).toContainText('403');
  expect(errors).toEqual([]);
});
