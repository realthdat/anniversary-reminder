// Same palette, timing and trajectories as the Android Canvas celebration.
function launchCheer(canvas) {
  const gen = ++cheerGen;
  const ctx = canvas.getContext('2d');
  const caption = canvas.parentElement.querySelector('div');
  let w, h, reach;
  function resize() {
    w = canvas.clientWidth; h = canvas.clientHeight;
    const dpr = Math.min(window.devicePixelRatio || 1, 3);
    canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    reach = Math.min(w, h);
  }
  resize();
  const shells = [
    { x: .50, y: .19, born: .55, color: '#ffd58a' },
    { x: .24, y: .25, born: 1.12, color: '#9adfff' },
    { x: .77, y: .22, born: 1.53, color: '#ffe7b6' },
    { x: .34, y: .15, born: 1.96, color: '#ffd58a' },
    { x: .67, y: .17, born: 2.35, color: '#9adfff' },
  ];
  let seed = 42;
  const random = () => ((seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0) / 4294967296);
  const embers = shells.flatMap(shell => Array.from({ length: 76 }, (_, n) => ({
    shell, angle: n * Math.PI * 2 / 76 + random() * .035,
    speed: .21 + random() * .43, life: 1.55 + random() * .95,
    size: .55 + random() * .65, phase: random() * 6.28,
  })));
  const clamp = value => Math.max(0, Math.min(1, value));
  function dot(x, y, r, color, alpha) {
    ctx.globalAlpha = clamp(alpha); ctx.fillStyle = color;
    ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
  }
  function glow(x, y, r, color, alpha) {
    const gradient = ctx.createRadialGradient(x, y, 0, x, y, r);
    gradient.addColorStop(0, color); gradient.addColorStop(1, color + '00');
    dot(x, y, r, gradient, alpha);
  }
  const t0 = performance.now();
  function toast(t) {
    const p = clamp(t / .85), ease = 1 - (1 - p) ** 3;
    const recoil = t > .85 ? Math.sin(Math.min(1, (t - .85) / .4) * Math.PI) : 0;
    const scale = Math.min(w / 390, h / 720);
    for (const side of [-1, 1]) {
      ctx.save();
      ctx.translate(w / 2 + side * (90 - 62 * ease + 5 * recoil) * scale, h * .68);
      ctx.scale(scale * side, scale);
      ctx.rotate((-16 * ease + 7 * recoil) * Math.PI / 180);
      ctx.globalAlpha = clamp(t / .25);
      ctx.strokeStyle = '#ffe9b5'; ctx.lineWidth = 5;
      ctx.beginPath(); ctx.roundRect(19, -20, 24, 32, 10); ctx.stroke();
      const beer = ctx.createLinearGradient(-24, 0, 24, 0);
      beer.addColorStop(0, '#d98716'); beer.addColorStop(.45, '#ffd66d'); beer.addColorStop(1, '#efa624');
      ctx.fillStyle = beer; ctx.beginPath(); ctx.roundRect(-24, -32, 48, 66, 9); ctx.fill();
      ctx.strokeStyle = '#fff0c5'; ctx.lineWidth = 2; ctx.stroke();
      ctx.fillStyle = '#ffffff55'; ctx.beginPath(); ctx.roundRect(-16, -19, 5, 41, 2); ctx.fill();
      for (const [x, y, r] of [[-17,-32,10],[-4,-37,12],[10,-35,11],[20,-30,8]]) {
        ctx.fillStyle = '#fff9e9'; ctx.beginPath(); ctx.arc(x,y,r,0,Math.PI*2); ctx.fill();
      }
      ctx.fillStyle = '#fff4c3';
      for (const [x,y] of [[-3,12],[10,-9],[5,24]]) { ctx.beginPath(); ctx.arc(x,y,2,0,Math.PI*2); ctx.fill(); }
      ctx.restore();
    }
    const impact = clamp((t - .85) / .45);
    if (t >= .85 && impact < 1) {
      ctx.strokeStyle = '#ffe6a6'; ctx.globalAlpha = 1 - impact; ctx.lineWidth = 2;
      for (let j=0;j<5;j++) {
        const a = Math.PI + j * Math.PI / 4, r = (14 + impact * 20) * scale;
        ctx.beginPath(); ctx.moveTo(w/2+Math.cos(a)*r,h*.68-44*scale+Math.sin(a)*r);
        ctx.lineTo(w/2+Math.cos(a)*(r+8*scale),h*.68-44*scale+Math.sin(a)*(r+8*scale)); ctx.stroke();
      }
    }
    ctx.globalAlpha = 1;
  }
  function frame(now) {
    if (gen !== cheerGen || !canvas.isConnected) return;
    if (w !== canvas.clientWidth || h !== canvas.clientHeight) resize();
    const t = (now - t0) / 1000;
    ctx.clearRect(0, 0, w, h); ctx.lineCap = 'round';
    for (const shell of shells) {
      const age = t - shell.born, ox = shell.x * w, oy = shell.y * h;
      if (age >= -.5 && age <= 0) {
        const p = clamp(1 + age / .5);
        const x = ox - reach * .04 * (1 - p), y = oy + reach * .34 * (1 - p) ** 2;
        for (let j = 1; j <= 10; j++) {
          const a = (1 - j / 11) * p;
          dot(x - j * reach * .0015, y + j * reach * .006, .5 + a * .5, shell.color, a * .65);
        }
        glow(x, y, 9, shell.color, .35 * p); dot(x, y, 1.3, '#fff5de', p);
      }
      if (age >= 0 && age <= .5) glow(ox, oy, reach * (.045 + age * .15), shell.color, .26 * (1 - age / .5));
    }
    for (const ember of embers) {
      const age = t - ember.shell.born;
      if (age <= 0 || age >= ember.life) continue;
      const position = time => {
        const travel = ember.speed * reach * (1 - Math.exp(-1.35 * time)) / 1.35;
        return [ember.shell.x * w + Math.cos(ember.angle) * travel,
          ember.shell.y * h + Math.sin(ember.angle) * travel * .86 + reach * .032 * time * time];
      };
      const [x, y] = position(age);
      const fade = (1 - age / ember.life) ** 2 * clamp((h * .43 - y) / (reach * .09));
      const alpha = fade * (age > .65 ? .65 + .35 * Math.sin(age * 19 + ember.phase) : 1);
      for (let j = 0; j < 5; j++) {
        const a = position(Math.max(0, age - j * .035)), b = position(Math.max(0, age - (j + 1) * .035));
        ctx.globalAlpha = alpha * (1 - j / 5) * .8;
        ctx.strokeStyle = ember.shell.color; ctx.lineWidth = ember.size;
        ctx.beginPath(); ctx.moveTo(...b); ctx.lineTo(...a); ctx.stroke();
      }
      glow(x, y, ember.size * 3.5, ember.shell.color, alpha * .18);
      dot(x, y, ember.size, ember.shell.color, alpha);
      dot(x, y, ember.size * .45, '#fffaed', alpha * .85);
    }
    ctx.globalAlpha = 1;
    toast(t);
    if (caption) caption.style.opacity = String(clamp((t - .85) / .22));
    if (t < 5.5) requestAnimationFrame(frame);
    else {
      cheerGen++;
      if (!demoCheer) dueToday().forEach(hit => cheered.add(hit.key));
      demoCheer = false;
      render();
    }
  }
  requestAnimationFrame(frame);
}
