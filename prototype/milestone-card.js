// Mirrors MilestoneCardArt.kt; text stays in the DOM for accessibility.
let milestoneObserver;
function paintMilestoneCard() {
  milestoneObserver?.disconnect();
  const card = document.querySelector('.hero');
  if (!card) return;
  const canvas = document.createElement('canvas');
  canvas.className = 'milestone-art';
  canvas.setAttribute('aria-hidden', 'true');
  card.prepend(canvas);
  const draw = () => {
    const w = card.clientWidth, h = card.clientHeight;
    if (!w || !h) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 3);
    canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr);
    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    const base = ctx.createLinearGradient(0, 0, w, h);
    base.addColorStop(0, '#294c57'); base.addColorStop(.5, '#1b3540'); base.addColorStop(1, '#142c37');
    ctx.fillStyle = base; ctx.fillRect(0, 0, w, h);
    const halo = ctx.createRadialGradient(w*.94, h*.22, 0, w*.94, h*.22, w*.70);
    halo.addColorStop(0, '#6ed5cc26'); halo.addColorStop(1, '#6ed5cc00');
    ctx.fillStyle = halo; ctx.fillRect(0, 0, w, h);
    const rings = ctx.createLinearGradient(0, 0, w, 0);
    rings.addColorStop(0, '#e7cf9e00'); rings.addColorStop(1, '#e7cf9e26');
    ctx.strokeStyle = rings; ctx.lineWidth = .75;
    for (const fraction of [.34, .46, .58]) {
      ctx.beginPath(); ctx.arc(w*1.10, h*.44, w*fraction, 0, Math.PI*2); ctx.stroke();
    }
    const veil = ctx.createLinearGradient(0, 0, w, 0);
    veil.addColorStop(0, '#233f49b3'); veil.addColorStop(1, '#233f4900');
    ctx.fillStyle = veil; ctx.fillRect(0, 0, w, h);
    const edge = ctx.createLinearGradient(0, 0, w, h);
    edge.addColorStop(0, '#e7cf9e40'); edge.addColorStop(1, '#e7cf9e08');
    ctx.strokeStyle = edge; ctx.lineWidth = 1;
    ctx.beginPath(); ctx.roundRect(.5, .5, w-1, h-1, 26); ctx.stroke();
  };
  milestoneObserver = new ResizeObserver(draw);
  milestoneObserver.observe(card);
  draw();
}
